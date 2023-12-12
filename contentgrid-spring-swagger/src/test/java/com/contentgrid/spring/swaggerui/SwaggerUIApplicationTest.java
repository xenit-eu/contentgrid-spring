package com.contentgrid.spring.swaggerui;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest
@ContextConfiguration(classes = SwaggerUIInitializerController.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
public class SwaggerUIApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void webjarsSwaggerUIwithoutVersionReturnsHttpOk() throws Exception {
        this.mockMvc.perform(get("/webjars/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML));

    }

    @Test
    void webjarsSwaggerUIwithInvalidVersionReturnsHttpNotFound() throws Exception {
        this.mockMvc.perform(get("/webjars/swagger-ui/1.2.3.4/index.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    void customSwaggerInitializer() throws Exception {
        this.mockMvc.perform(get("/webjars/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())

                // should NOT serve any default config
                .andExpect(content().string(not(containsString("petstore"))))

                // but use the contentgrid url: "/openapi.yml"
                .andExpect(content().string(containsString("url: \"/openapi.yml\"")));
    }


}
