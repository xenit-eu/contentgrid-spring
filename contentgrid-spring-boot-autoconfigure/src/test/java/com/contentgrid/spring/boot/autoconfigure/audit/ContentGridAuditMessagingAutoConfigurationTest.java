package com.contentgrid.spring.boot.autoconfigure.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.audit.handler.messaging.MessageSendingAuditHandler;
import com.contentgrid.spring.boot.autoconfigure.messaging.ContentGridMessagingAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.RabbitMQContainer;

class ContentGridAuditMessagingAutoConfigurationTest {

    private static final String AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER = "auditEventToCloudEventMessageConverter";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    ContentGridAuditMessagingAutoConfiguration.class,
                    ContentGridAuditLoggingAutoConfiguration.class,
                    ContentGridMessagingAutoConfiguration.class,
                    RepositoryRestMvcAutoConfiguration.class
            ));

    @Test
    void messagingHandlerEnabledWhenMessagingTemplateIsAvailable() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSendingAuditHandler.class);
                });
    }

    @Test
    void messagingHandlerDisabledWithoutConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(MessageSendingAuditHandler.class);
        });
    }

    @Test
    void messagingHandlerDisabledByProperty() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .withPropertyValues("contentgrid.audit.messaging.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MessageSendingAuditHandler.class);
                });
    }

    @Test
    void defaultConverterUsedWhenNoSourceConfigured() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER);
                });
    }

    @Test
    void cloudEventsConverterUsedWhenSourceConfigured() {
        contextRunner.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(AmqpServiceConnection.class)
                .withPropertyValues("contentgrid.audit.messaging.source=https://example.com/abc")
                .run(context -> {
                    assertThat(context).hasBean(AUDIT_EVENT_TO_CLOUD_EVENT_MESSAGE_CONVERTER);
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