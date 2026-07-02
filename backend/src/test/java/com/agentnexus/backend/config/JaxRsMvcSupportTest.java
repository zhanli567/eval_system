package com.agentnexus.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentnexus.backend.common.GlobalExceptionHandler;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = JaxRsMvcSupportTest.SampleController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class JaxRsMvcSupportTest {
  @Autowired
  private MockMvc mockMvc;

  @Test
  void bindsJakartaPathAndQueryParams() throws Exception {
    mockMvc.perform(get("/sample/abc").queryParam("page", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("abc"))
        .andExpect(jsonPath("$.page").value(3));
  }

  @Test
  void bindsJakartaHeaderParams() throws Exception {
    mockMvc.perform(get("/sample/header").header("Cookie", "sid=abc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cookie").value("sid=abc"));
  }

  @Test
  void bindsUnannotatedJsonBodyEntity() throws Exception {
    mockMvc.perform(post("/sample")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"demo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("demo"));
  }

  @Test
  void keepsResponseStatusExceptions() throws Exception {
    mockMvc.perform(get("/sample/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(403));
  }

  @Component
  @ResponseBody
  @Path("/sample")
  static class SampleController {
    @GET
    @Path("/{id}")
    Map<String, Object> getSample(
        @PathParam("id") String id,
        @QueryParam("page") @DefaultValue("1") int page
    ) {
      return Map.of("id", id, "page", page);
    }

    @GET
    @Path("/header")
    Map<String, Object> getHeader(@HeaderParam("Cookie") String cookie) {
      return Map.of("cookie", cookie);
    }

    @GET
    @Path("/forbidden")
    Map<String, Object> forbidden() {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
    }

    @POST
    @Path("")
    Map<String, Object> createSample(SampleRequest request) {
      return Map.of("name", request.name());
    }
  }

  record SampleRequest(String name) {
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import({WebConfig.class, SampleController.class})
  static class TestApplication {
  }
}
