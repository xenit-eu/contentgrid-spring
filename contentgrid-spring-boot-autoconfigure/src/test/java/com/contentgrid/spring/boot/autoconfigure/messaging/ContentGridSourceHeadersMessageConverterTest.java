package com.contentgrid.spring.boot.autoconfigure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;

class ContentGridSourceHeadersMessageConverterTest {

    @Test
    void omitSourceHeadersWhenMissing() {
        var properties = new ContentGridMessagingSystemProperties();
        var converter = createConverter(properties);
        assertThat(converter.toMessage("abcdef", null)).satisfies(message -> {
            assertThat(message.getPayload()).isEqualTo("abcdef");
            assertThat(message.getHeaders()).doesNotContainKeys("applicationId", "deploymentId");
        });
    }

    @Test
    void addSourceHeadersWhenConfigured() {
        var properties = new ContentGridMessagingSystemProperties();
        properties.setApplicationId("application-id");
        properties.setDeploymentId("deployment-id");
        var converter = createConverter(properties);

        assertThat(converter.toMessage("abcdef", null)).satisfies(message -> {
            assertThat(message.getPayload()).isEqualTo("abcdef");
            assertThat(message.getHeaders()).containsEntry("applicationId", "application-id")
                    .containsEntry("deploymentId", "deployment-id");
        });
    }

    @Test
    void keepsExistingHeaders() {
        var properties = new ContentGridMessagingSystemProperties();
        properties.setApplicationId("application-id");
        properties.setDeploymentId("deployment-id");
        var converter = createConverter(properties);

        assertThat(converter.toMessage("abcdef", new MessageHeaders(Map.of("test", "test123")))).satisfies(message -> {
            assertThat(message.getPayload()).isEqualTo("abcdef");
            assertThat(message.getHeaders()).containsEntry("applicationId", "application-id")
                    .containsEntry("deploymentId", "deployment-id")
                    .containsEntry("test", "test123");
        });

    }

    @NotNull
    private static ContentGridSourceHeadersMessageConverter createConverter(
            ContentGridMessagingSystemProperties properties) {
        return new ContentGridSourceHeadersMessageConverter(
                new CompositeMessageConverter(List.of(new SimpleMessageConverter())),
                properties
        );
    }

}