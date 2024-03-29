package com.contentgrid.spring.boot.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.boot.autoconfigure.security.AnonymousHttpConfigurer.AnonymousUsernamePasswordAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

class AnonymousHttpConfigurerTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OAuth2ResourceServerAutoConfiguration.class, SecurityAutoConfiguration.class
            ));

    @Test
    void checkDefaults() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context.getBean(SecurityFilterChain.class).getFilters()).noneMatch(filter ->
                            filter instanceof AnonymousUsernamePasswordAuthenticationFilter
                    );
                });
    }

    @Test
    void checkWithProperty() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withPropertyValues(
                        "contentgrid.security.unauthenticated.allow=true",
                        "contentgrid.security.csrf.disabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context.getBean(SecurityFilterChain.class).getFilters()).anyMatch(filter ->
                            filter instanceof AnonymousUsernamePasswordAuthenticationFilter
                    );
                });
    }

    @Test
    void checkWithoutOAuth2ResourceServer() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
                .withPropertyValues(
                        "contentgrid.security.unauthenticated.allow=true",
                        "contentgrid.security.csrf.disabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context.getBean(SecurityFilterChain.class).getFilters()).anyMatch(filter ->
                            filter instanceof AnonymousUsernamePasswordAuthenticationFilter
                    );
                });
    }

    @Test
    void checkWithoutSecurityFilterChain() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
                .withPropertyValues(
                        "contentgrid.security.unauthenticated.allow=true",
                        "contentgrid.security.csrf.disabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
                });
    }
}