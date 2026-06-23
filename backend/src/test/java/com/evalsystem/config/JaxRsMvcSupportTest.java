package com.evalsystem.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ResponseBody;

@WebMvcTest(controllers = JaxRsMvcSupportTest.SampleController.class)
@Import(WebConfig.class)
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
  void bindsUnannotatedJsonBodyEntity() throws Exception {
    mockMvc.perform(post("/sample")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"demo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("demo"));
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
