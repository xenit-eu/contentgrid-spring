package com.contentgrid.spring.boot.autoconfigure.swagger.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.swagger.ui.SwaggerUIInitializerController;
import com.contentgrid.spring.swagger.ui.SwaggerUIRestConfiguration;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

class SwaggerUIRestAutoConfigurationTest {

    WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ServletWebServerFactoryAutoConfiguration.class,
                    DispatcherServletAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    SwaggerUIRestAutoConfiguration.class
            ));


    @Test
    void autoConfigurationLoaded() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SwaggerUIRestAutoConfiguration.class);
            assertThat(context).hasSingleBean(SwaggerUIRestConfiguration.class);
            assertThat(context).hasSingleBean(SwaggerUIInitializerController.class);

            webTestClient(context)
                    .get().uri("/webjars/swagger-ui/swagger-initializer.js")
                    .exchange()
                    .expectBody(String.class)
                    .value(StringContains.containsString("/openapi.yml"));
        });
    }

    @Test
    void conditionalOnSwaggerUIClass() {
        runner.withClassLoader(new FilteredClassLoader(SwaggerUIRestConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SwaggerUIRestAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(SwaggerUIRestConfiguration.class);
                    assertThat(context).doesNotHaveBean(SwaggerUIInitializerController.class);

                    // swagger-ui autoconfiguration not loaded, load default swagger-init petstore config
                    webTestClient(context)
                            .get().uri("/webjars/swagger-ui/swagger-initializer.js")
                            .exchange()
                            .expectBody(String.class)
                            .value(StringContains.containsString("https://petstore.swagger.io/v2/swagger.json"));
                });
    }

    static WebTestClient webTestClient(WebApplicationContext context) {
        return MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

}