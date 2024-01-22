package com.contentgrid.spring.cloudevents.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

class CloudEventMessageConverterTest {

    static MessageConverter CONVERTER = new CloudEventMessageConverter(new SimpleMessageConverter());

    @Test
    void encodeCloudEvent() {
        var event = CloudEventBuilder.v1()
                .withId("abc")
                .withType("com.contentgrid.test")
                .withSource(URI.create("http://example.com/ns"))
                .withData("text/plain", "test".getBytes(StandardCharsets.UTF_8))
                .withSubject("abc-def")
                .build();
        var message = CONVERTER.toMessage(event, new MessageProperties());

        assertThat(message.getBody()).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
        assertThat(message.getMessageProperties().getHeaders()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "cloudEvents:specversion", "1.0",
                "cloudEvents:id", "abc",
                "cloudEvents:type", "com.contentgrid.test",
                "cloudEvents:source", "http://example.com/ns",
                "cloudEvents:subject", "abc-def",
                "cloudEvents:datacontenttype", "text/plain"
        ));
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void decodeCloudEvent() {
        var message = new Message("test".getBytes(StandardCharsets.UTF_8));
        message.getMessageProperties().setHeaders(Map.of(
                "cloudEvents:specversion", "1.0",
                "cloudEvents:id", "abc",
                "cloudEvents:type", "com.contentgrid.test",
                "cloudEvents:source", "http://example.com/ns",
                "cloudEvents:subject", "abc-def",
                "cloudEvents:datacontenttype", "text/plain"
        ));

        var event = CONVERTER.fromMessage(message);

        assertThat(event).isInstanceOfSatisfying(CloudEvent.class, ce -> {
            assertThat(ce.getSource()).isEqualTo(URI.create("http://example.com/ns"));
            assertThat(ce.getId()).isEqualTo("abc");
            assertThat(ce.getType()).isEqualTo("com.contentgrid.test");
            assertThat(ce.getSubject()).isEqualTo("abc-def");
            assertThat(ce.getData().toBytes()).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
        });
    }

}