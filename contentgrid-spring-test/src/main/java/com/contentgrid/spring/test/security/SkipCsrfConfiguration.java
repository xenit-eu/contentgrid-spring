package com.contentgrid.spring.test.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class SkipCsrfConfiguration {
    @Bean
    MockMvcBuilderCustomizer skipCsrfBuilderCustomizer() {
        return new SkipCsrfBuilderCustomizer();
    }

    @RequiredArgsConstructor
    static class SkipCsrfBuilderCustomizer implements MockMvcBuilderCustomizer {
        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(new SkipCsrfMockMvcConfigurer());
        }
    }

    @RequiredArgsConstructor
    static class SkipCsrfMockMvcConfigurer implements MockMvcConfigurer {
        @Override
        public RequestPostProcessor beforeMockMvcCreated(
                ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {
            return request -> {
                CsrfFilter.skipRequest(request);
                return request;
            };
        }
    }

}
