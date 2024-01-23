package com.contentgrid.spring.boot.autoconfigure.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.audit.handler.amqp.AmqpAuditHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.RabbitMQContainer;

class ContentGridAmqpAuditAutoConfigurationTest {

    private static final String AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER = "auditEventToCloudEventMessageConverterRabbitTemplateCustomizer";
    private static final String AUDIT_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER = "auditEventMessageConverterRabbitTemplateCustomizer";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ContentGridAmqpAuditAutoConfiguration.class,
                    ContentGridAuditLoggingAutoConfiguration.class,
                    RepositoryRestMvcAutoConfiguration.class
            ));

    @Test
    void amqpHandlerEnabledWhenAmqpTemplateIsAvailable() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AmqpAuditHandler.class);
                });
    }

    @Test
    void amqpHandlerDisabledWithoutConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AmqpAuditHandler.class);
        });
    }

    @Test
    void amqpHandlerDisabledByProperty() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .withPropertyValues("contentgrid.audit.amqp.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AmqpAuditHandler.class);
                });
    }

    @Test
    void defaultConverterUsedWhenNoSourceConfigured() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER);
                    assertThat(context).hasBean(AUDIT_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER);
                });
    }

    @Test
    void cloudEventsConverterUsedWhenSourceConfigured() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .withPropertyValues("contentgrid.audit.amqp.source=https://example.com/abc")
                .run(context -> {
                    assertThat(context).hasBean(
                            AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER);
                    assertThat(context).doesNotHaveBean(AUDIT_EVENT_MESSAGE_CONVERTER_RABBIT_TEMPLATE_CUSTOMIZER);
                });
    }


    @Configuration(proxyBeanMethods = false)
    private static class AmqpServiceConnection {

        @ServiceConnection
        @Bean
        RabbitMQContainer rabbitMQContainer() {
            return new RabbitMQContainer();
        }
    }

}