package com.contentgrid.spring.audit.handler.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationItemAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

class AuditEventToCloudEventMessageConverterTest {

    private final static Message EMPTY_MESSAGE = new GenericMessage<>(new byte[0]);

    @Test
    void publishesBasicEvent() {
        var upstreamConverter = Mockito.mock(MessageConverter.class);
        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = BasicAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/profile/abc")
                .responseStatus(203)
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo("cloud.contentgrid.audit.basic");
            assertThat(cloudEvent.getSubject()).isEqualTo("/profile/abc");
            assertThat(cloudEvent.getData().toBytes()).containsSequence(
                    "/profile/abc".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void publishesItemCreateEvent() {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntityItemAuditEvent.builder()
                .requestMethod("POST")
                .requestUri("/abcs")
                .responseLocation("/abcs/123")
                .responseStatus(203)
                .operation(Operation.CREATE)
                .domainType(Object.class)
                .id("123")
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo("cloud.contentgrid.audit.entity.create");
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs/123");
            assertThat(cloudEvent.getData().toBytes()).containsSequence(
                    "\"id\":\"123\"".getBytes(StandardCharsets.UTF_8));
        });
    }

    @ParameterizedTest
    @CsvSource({
            "READ",
            "UPDATE",
            "DELETE"
    })
    void publishesItemEvent(EntityItemAuditEvent.Operation operation) {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntityItemAuditEvent.builder()
                .requestMethod("POST")
                .requestUri("/abcs/123")
                .responseStatus(200)
                .operation(operation)
                .domainType(Object.class)
                .id("123")
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo("cloud.contentgrid.audit.entity." + operation.name().toLowerCase(
                    Locale.ROOT));
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs/123");
            assertThat(cloudEvent.getData().toBytes()).containsSequence(
                    "\"id\":\"123\"".getBytes(StandardCharsets.UTF_8));
        });
    }

    @ParameterizedTest
    @CsvSource({
            "READ",
            "UPDATE",
            "DELETE"
    })
    void publishesRelationEvent(AbstractEntityRelationAuditEvent.Operation operation) {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntityRelationAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/abcs/123/xyz")
                .responseStatus(200)
                .operation(operation)
                .domainType(Object.class)
                .id("123")
                .relationName("xyz")
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo(
                    "cloud.contentgrid.audit.entity.relation." + operation.name().toLowerCase(
                            Locale.ROOT));
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs/123/xyz");
        });
    }

    @ParameterizedTest
    @CsvSource({
            "READ",
            "UPDATE",
            "DELETE"
    })
    void publishesRelationItemEvent(AbstractEntityRelationAuditEvent.Operation operation) {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntityRelationItemAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/abcs/123/xyz/ZZZ")
                .responseStatus(200)
                .operation(operation)
                .domainType(Object.class)
                .id("123")
                .relationName("xyz")
                .relationId("ZZZ")
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo(
                    "cloud.contentgrid.audit.entity.relation." + operation.name().toLowerCase(
                            Locale.ROOT));
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs/123/xyz/ZZZ");
        });
    }

    @ParameterizedTest
    @CsvSource({
            "READ",
            "UPDATE",
            "DELETE"
    })
    void publishesContentEvent(EntityContentAuditEvent.Operation operation) {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntityContentAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/abcs/123/xyz")
                .responseStatus(200)
                .operation(operation)
                .domainType(Object.class)
                .id("123")
                .contentName("xyz")
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo(
                    "cloud.contentgrid.audit.entity.content." + operation.name().toLowerCase(
                            Locale.ROOT));
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs/123/xyz");
        });
    }

    @Test
    void publishesSearchEvent() {
        var upstreamConverter = Mockito.mock(MessageConverter.class);

        var cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);

        Mockito.when(upstreamConverter.toMessage(cloudEventCaptor.capture(), Mockito.any()))
                .thenReturn(EMPTY_MESSAGE);
        var handler = new AuditEventToCloudEventMessageConverter(
                upstreamConverter,
                new ObjectMapper()::writeValueAsBytes,
                URI.create("https://contentgrid.com/audit-source")
        );

        var event = EntitySearchAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/abcs")
                .responseStatus(200)
                .queryParameters(Map.of("xyz", List.of("123")))
                .build();

        handler.toMessage(event, new MessageHeaders(null));

        assertThat(cloudEventCaptor.getValue()).satisfies(cloudEvent -> {
            assertThat(cloudEvent.getSource()).isEqualTo(URI.create("https://contentgrid.com/audit-source"));
            assertThat(cloudEvent.getType()).isEqualTo("cloud.contentgrid.audit.entity.list");
            assertThat(cloudEvent.getSubject()).isEqualTo("/abcs");
        });
    }
}