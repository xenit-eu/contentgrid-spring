package com.contentgrid.spring.boot.autoconfigure.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import com.contentgrid.spring.audit.handler.LoggingAuditHandler;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ContentGridAuditLoggingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ContentGridAuditLoggingAutoConfiguration.class,
                    RepositoryRestMvcAutoConfiguration.class
            ));


    @Test
    void automaticallyEnabledOnMissingHandler() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LoggingAuditHandler.class);
        });
    }

    @Test
    void automaticallyDisabledOnExistingHandler() {
        contextRunner.with(customHandler())
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingAuditHandler.class);
                });
    }

    @Test
    void manuallyEnabledWithMissingHandler() {
        contextRunner.with(loggingEnabled(true))
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingAuditHandler.class);
                });
    }

    @Test
    void manuallyEnabledWithExistingHandler() {
        contextRunner.with(loggingEnabled(true))
                .with(customHandler())
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingAuditHandler.class);
                });
    }

    @Test
    void manuallyDisabledWithMissingHandler() {
        contextRunner.with(loggingEnabled(false))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingAuditHandler.class);
                });
    }

    @Test
    void manuallyDisabledWithExistingHandler() {
        contextRunner.with(loggingEnabled(false))
                .with(customHandler())
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LoggingAuditHandler.class);
                });
    }

    private static class CustomAuditHandler implements AuditEventHandler {

        @Override
        public void handle(AbstractAuditEvent auditEvent) {

        }
    }

    private static UnaryOperator<WebApplicationContextRunner> loggingEnabled(boolean value) {
        return c -> c.withPropertyValues("contentgrid.audit.log.enabled=" + Boolean.toString(value));
    }

    private static UnaryOperator<WebApplicationContextRunner> customHandler() {
        return c -> c.withBean(CustomAuditHandler.class);
    }

}