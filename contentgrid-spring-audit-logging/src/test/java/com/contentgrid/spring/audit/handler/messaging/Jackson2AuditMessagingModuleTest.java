package com.contentgrid.spring.audit.handler.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationItemAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Jackson2AuditMessagingModuleTest {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new Jackson2AuditMessagingModule());
    }

    @Test
    void serializesBasicAuditEvent()
            throws JsonProcessingException {
        var auditEvent = BasicAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/profile/my-entities")
                .responseStatus(200)
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "request.method": "GET",
                    "request.uri": "/profile/my-entities",
                    "response.status": 200
                }
                """));
    }

    @ParameterizedTest
    @CsvSource({
            "CREATE,entity.create",
            "READ,entity.read",
            "UPDATE,entity.update",
            "DELETE,entity.delete"
    })
    void serializesItemAuditEvent(EntityItemAuditEvent.Operation operation, String serializedOperation)
            throws JsonProcessingException {
        var auditEvent = EntityItemAuditEvent.builder()
                .operation(operation)
                .domainType(MyEntity.class)
                .id("1234")
                .requestMethod("POST")
                .requestUri("/my-entities")
                .responseStatus(201)
                .responseLocation("/my-entities/1234")
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "operation": "%s",
                    "request.method": "POST",
                    "request.uri": "/my-entities",
                    "response.status": 201,
                    "response.location": "/my-entities/1234",
                    "subject.type": "MyEntity",
                    "subject.id": "1234"
                }
                """.formatted(serializedOperation)));
    }


    @Test
    void serializesSearchAuditEvent() throws JsonProcessingException {
        var auditEvent = EntitySearchAuditEvent.builder()
                .requestMethod("GET")
                .requestUri("/my-entities")
                .responseStatus(200)
                .domainType(MyEntity.class)
                .queryParameters(Map.of("xyz", List.of("abc")))
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "operation": "entity.list",
                    "request.method": "GET",
                    "request.uri": "/my-entities",
                    "response.status": 200,
                    "subject.type": "MyEntity",
                    "search": {
                        "xyz": ["abc"]
                    }
                }
                """));
    }

    @ParameterizedTest
    @CsvSource({
            "READ,relation.read",
            "UPDATE,relation.update",
            "DELETE,relation.delete"
    })
    void serializesRelationAuditEvent(AbstractEntityRelationAuditEvent.Operation operation, String serializedOperation)
            throws JsonProcessingException {
        var auditEvent = EntityRelationAuditEvent.builder()
                .operation(operation)
                .requestMethod("GET")
                .requestUri("/my-entities/123/xyz")
                .responseStatus(200)
                .domainType(MyEntity.class)
                .id("123")
                .relationName("xyz")
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "operation": "%s",
                    "request.method": "GET",
                    "request.uri": "/my-entities/123/xyz",
                    "response.status": 200,
                    "subject.type": "MyEntity",
                    "subject.id": "123",
                    "subject.relation": "xyz"
                }
                """.formatted(serializedOperation)));
    }

    @ParameterizedTest
    @CsvSource({
            "READ,relation.read",
            "UPDATE,relation.update",
            "DELETE,relation.delete"
    })
    void serializesRelationItemAuditEvent(AbstractEntityRelationAuditEvent.Operation operation,
            String serializedOperation)
            throws JsonProcessingException {
        var auditEvent = EntityRelationItemAuditEvent.builder()
                .operation(operation)
                .requestMethod("GET")
                .requestUri("/my-entities/123/xyz/555")
                .responseStatus(200)
                .domainType(MyEntity.class)
                .id("123")
                .relationName("xyz")
                .relationId("555")
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "operation": "%s",
                    "request.method": "GET",
                    "request.uri": "/my-entities/123/xyz/555",
                    "response.status": 200,
                    "subject.type": "MyEntity",
                    "subject.id": "123",
                    "subject.relation": "xyz",
                    "subject.relation.id": "555"
                }
                """.formatted(serializedOperation)));
    }

    @ParameterizedTest
    @CsvSource({
            "READ,content.read",
            "UPDATE,content.update",
            "DELETE,content.delete"
    })
    void serializesContentAuditEvent(EntityContentAuditEvent.Operation operation, String serializedOperation)
            throws JsonProcessingException {
        var auditEvent = EntityContentAuditEvent.builder()
                .operation(operation)
                .requestMethod("GET")
                .requestUri("/my-entities/123/xyz")
                .responseStatus(200)
                .domainType(MyEntity.class)
                .id("123")
                .contentName("xyz")
                .build();

        assertThat((Object) OBJECT_MAPPER.valueToTree(auditEvent)).isEqualTo(OBJECT_MAPPER.readTree("""
                {
                    "operation": "%s",
                    "request.method": "GET",
                    "request.uri": "/my-entities/123/xyz",
                    "response.status": 200,
                    "subject.type": "MyEntity",
                    "subject.id": "123",
                    "subject.content": "xyz"
                }
                """.formatted(serializedOperation)));

    }


    private static class MyEntity {

    }

}