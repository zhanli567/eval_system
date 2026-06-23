package com.evalsystem.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Configuration
public class WebConfig implements WebMvcConfigurer, WebMvcRegistrations {
  private static final ConversionService CONVERSION_SERVICE = new DefaultFormattingConversionService();

  private final ObjectMapper objectMapper;

  public WebConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
    return new JaxRsRequestMappingHandlerMapping();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new PathParamResolver());
    resolvers.add(new QueryParamResolver());
    resolvers.add(new FormParamResolver());
    resolvers.add(new EntityBodyResolver(objectMapper));
  }

  private static class JaxRsRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    @Override
    protected boolean isHandler(Class<?> beanType) {
      return super.isHandler(beanType) || AnnotatedElementUtils.hasAnnotation(beanType, Path.class);
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
      RequestMappingInfo springInfo = super.getMappingForMethod(method, handlerType);
      return springInfo == null ? createJaxRsInfo(method, handlerType) : springInfo;
    }

    private RequestMappingInfo createJaxRsInfo(Method method, Class<?> handlerType) {
      RequestMethod[] methods = requestMethods(method);
      if (methods.length == 0) {
        return null;
      }
      RequestMappingInfo typeInfo = pathInfo(AnnotatedElementUtils.findMergedAnnotation(handlerType, Path.class));
      RequestMappingInfo methodInfo = pathInfo(AnnotatedElementUtils.findMergedAnnotation(method, Path.class), methods);
      return typeInfo.combine(methodInfo);
    }

    private RequestMappingInfo pathInfo(Path path, RequestMethod... methods) {
      RequestMappingInfo.Builder builder = RequestMappingInfo.paths(path == null ? "" : path.value())
          .options(getBuilderConfiguration());
      if (methods.length > 0) {
        builder.methods(methods);
      }
      return builder.build();
    }

    private RequestMethod[] requestMethods(Method method) {
      List<RequestMethod> methods = new ArrayList<>();
      for (Annotation annotation : method.getAnnotations()) {
        HttpMethod httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class);
        if (httpMethod != null) {
          methods.add(RequestMethod.valueOf(httpMethod.value()));
        }
      }
      return methods.toArray(RequestMethod[]::new);
    }
  }

  private abstract static class NamedValueResolver implements HandlerMethodArgumentResolver {
    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        org.springframework.web.bind.support.WebDataBinderFactory binderFactory
    ) {
      return convert(defaultValue(parameter, rawValue(parameter, webRequest)), parameter);
    }

    protected abstract String rawValue(MethodParameter parameter, NativeWebRequest webRequest);

    private String defaultValue(MethodParameter parameter, String value) {
      DefaultValue defaultValue = parameter.getParameterAnnotation(DefaultValue.class);
      return value == null && defaultValue != null ? defaultValue.value() : value;
    }

    private Object convert(String value, MethodParameter parameter) {
      if (value == null) {
        return parameter.getParameterType().isPrimitive() ? Array.get(Array.newInstance(parameter.getParameterType(), 1), 0) : null;
      }
      return CONVERSION_SERVICE.convert(value, TypeDescriptor.valueOf(String.class), new TypeDescriptor(parameter));
    }
  }

  private static class PathParamResolver extends NamedValueResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(PathParam.class);
    }

    @Override
    protected String rawValue(MethodParameter parameter, NativeWebRequest webRequest) {
      PathParam annotation = parameter.getParameterAnnotation(PathParam.class);
      HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
      Map<?, ?> variables = request == null ? Map.of() : (Map<?, ?>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
      Object value = variables == null ? null : variables.get(annotation.value());
      return value == null ? null : value.toString();
    }
  }

  private static class QueryParamResolver extends NamedValueResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(QueryParam.class);
    }

    @Override
    protected String rawValue(MethodParameter parameter, NativeWebRequest webRequest) {
      QueryParam annotation = parameter.getParameterAnnotation(QueryParam.class);
      return webRequest.getParameter(annotation.value());
    }
  }

  private static class FormParamResolver extends NamedValueResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(FormParam.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        org.springframework.web.bind.support.WebDataBinderFactory binderFactory
    ) {
      FormParam annotation = parameter.getParameterAnnotation(FormParam.class);
      if (MultipartFile.class.isAssignableFrom(parameter.getParameterType())) {
        MultipartHttpServletRequest request = webRequest.getNativeRequest(MultipartHttpServletRequest.class);
        return request == null ? null : request.getFile(annotation.value());
      }
      return super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    @Override
    protected String rawValue(MethodParameter parameter, NativeWebRequest webRequest) {
      FormParam annotation = parameter.getParameterAnnotation(FormParam.class);
      return webRequest.getParameter(annotation.value());
    }
  }

  private static class EntityBodyResolver implements HandlerMethodArgumentResolver {
    private final ObjectMapper objectMapper;

    EntityBodyResolver(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      Class<?> type = parameter.getParameterType();
      return AnnotatedElementUtils.hasAnnotation(parameter.getContainingClass(), Path.class)
          && !BeanUtils.isSimpleProperty(type)
          && !MultipartFile.class.isAssignableFrom(type)
          && !RedirectAttributes.class.isAssignableFrom(type)
          && !hasJaxRsAnnotation(parameter);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        org.springframework.web.bind.support.WebDataBinderFactory binderFactory
    ) throws Exception {
      HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
      if (request == null || request.getContentLengthLong() == 0) {
        return null;
      }
      try (InputStream body = request.getInputStream()) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(parameter.getGenericParameterType());
        return objectMapper.readValue(body, javaType);
      }
    }

    private boolean hasJaxRsAnnotation(MethodParameter parameter) {
      for (Annotation annotation : parameter.getParameterAnnotations()) {
        Package annotationPackage = annotation.annotationType().getPackage();
        if (annotationPackage != null && annotationPackage.getName().startsWith("jakarta.ws.rs")) {
          return true;
        }
      }
      return false;
    }
  }
}
