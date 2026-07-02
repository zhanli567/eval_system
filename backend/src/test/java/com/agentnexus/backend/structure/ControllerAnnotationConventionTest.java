package com.agentnexus.backend.structure;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentnexus.backend.dataset.api.DatasetController;
import com.agentnexus.backend.evaluator.api.EvaluatorController;
import com.agentnexus.backend.remoteCall.api.PlatformIntegrationController;
import com.agentnexus.backend.tag.api.TagController;
import com.agentnexus.backend.task.api.TaskController;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

class ControllerAnnotationConventionTest {
  private static final List<Class<?>> CONTROLLERS = List.of(
      DatasetController.class,
      TagController.class,
      EvaluatorController.class,
      TaskController.class,
      PlatformIntegrationController.class
  );

  @Test
  void controllersUseComponentAndJakartaPath() {
    CONTROLLERS.forEach(controller -> {
      assertThat(controller.isAnnotationPresent(Component.class)).isTrue();
      assertThat(controller.isAnnotationPresent(Path.class)).isTrue();
      assertThat(controller.isAnnotationPresent(RestController.class)).isFalse();
      assertThat(controller.isAnnotationPresent(RequestMapping.class)).isFalse();
    });
  }

  @Test
  void controllerMethodsUseJakartaHttpMethodAndPath() {
    CONTROLLERS.forEach(controller -> controllerMethods(controller).forEach(method -> {
      assertThat(method.isAnnotationPresent(Path.class)).isTrue();
      assertThat(hasJakartaHttpMethod(method)).isTrue();
      assertThat(method.isAnnotationPresent(GetMapping.class)).isFalse();
      assertThat(method.isAnnotationPresent(PostMapping.class)).isFalse();
      assertThat(method.isAnnotationPresent(PutMapping.class)).isFalse();
      assertThat(method.isAnnotationPresent(DeleteMapping.class)).isFalse();
    }));
  }

  @Test
  void controllerParametersUseJakartaAnnotationsOrBodyEntity() {
    CONTROLLERS.forEach(controller -> controllerMethods(controller).forEach(method -> {
      for (Parameter parameter : method.getParameters()) {
        assertThat(parameter.isAnnotationPresent(PathVariable.class)).isFalse();
        assertThat(parameter.isAnnotationPresent(RequestParam.class)).isFalse();
        assertThat(parameter.isAnnotationPresent(RequestBody.class)).isFalse();

        if (MultipartFile.class.isAssignableFrom(parameter.getType())) {
          assertThat(parameter.isAnnotationPresent(FormParam.class)).isTrue();
        } else if (isSimpleParameter(parameter)) {
          assertThat(hasAnyAnnotation(parameter, PathParam.class, QueryParam.class, HeaderParam.class, FormParam.class)).isTrue();
        }
      }
    }));
  }

  private static List<Method> controllerMethods(Class<?> controller) {
    return List.of(controller.getDeclaredMethods()).stream()
        .filter(method -> method.getDeclaringClass() == controller)
        .toList();
  }

  private static boolean hasJakartaHttpMethod(Method method) {
    return hasAnyAnnotation(method, GET.class, POST.class, PUT.class, DELETE.class);
  }

  @SafeVarargs
  private static boolean hasAnyAnnotation(Method method, Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      if (method.isAnnotationPresent(annotation)) {
        return true;
      }
    }
    return false;
  }

  @SafeVarargs
  private static boolean hasAnyAnnotation(Parameter parameter, Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      if (parameter.isAnnotationPresent(annotation)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSimpleParameter(Parameter parameter) {
    Class<?> type = parameter.getType();
    return type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type) || type == Boolean.class
        || parameter.isAnnotationPresent(DefaultValue.class);
  }
}
