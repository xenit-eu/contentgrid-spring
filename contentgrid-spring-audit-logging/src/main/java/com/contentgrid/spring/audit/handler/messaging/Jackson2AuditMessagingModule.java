package com.contentgrid.spring.audit.handler.messaging;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.EnumSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class Jackson2AuditMessagingModule extends SimpleModule {

    public Jackson2AuditMessagingModule() {
        super("contentgrid-audit-messaging",
                new Version(1, 0, 0, null, "com.contentgrid.spring", "contentgrid-spring-audit-logging"));

        setMixInAnnotation(AbstractAuditEvent.class, AbstractAuditEventMixin.class);
    }

    @JsonPropertyOrder({
            "operation",
            "request.method",
            "request.uri",
            "response.status",
            "response.location",
            "subject.type",
            "subject.id",
            "subject.relation",
            "subject.relation.id",
            "subject.content",
            "search"
    })
    private abstract static class AbstractAuditEventMixin {

        @JsonProperty("operation")
        @JsonSerialize(using = AuditEventOperationSerializer.class)
        public abstract Object getOperation();

        @JsonProperty("request.method")
        public abstract String getRequestMethod();

        @JsonProperty("request.uri")
        public abstract String getRequestUri();

        @JsonProperty("response.status")
        public abstract int getResponseStatus();

        @JsonProperty("response.location")
        @JsonInclude(Include.NON_NULL)
        public abstract String getResponseLocation();

        @JsonProperty("subject.type")
        @JsonSerialize(using = EntityTypeToNameSerializer.class)
        @JsonInclude(Include.NON_NULL)
        abstract Class<?> getDomainType();

        @JsonProperty("subject.id")
        @JsonInclude(Include.NON_NULL)
        abstract String getId();

        @JsonProperty("subject.relation")
        @JsonInclude(Include.NON_NULL)
        abstract String getRelationName();

        @JsonProperty("subject.relation.id")
        @JsonInclude(Include.NON_NULL)
        abstract String getRelationId();

        @JsonProperty("subject.content")
        @JsonInclude(Include.NON_NULL)
        abstract String getContentName();

        @JsonProperty("search")
        @JsonInclude(Include.NON_NULL)
        abstract Object getQueryParameters();
    }

    private static class EntityTypeToNameSerializer extends ClassSerializer {

        @Override
        public void serialize(Class<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getSimpleName());
        }
    }

    private static class AuditEventOperationSerializer extends StdSerializer<Enum<?>> {

        private static final Map<Enum<?>, String> TYPES = Map.of(
                EntityItemAuditEvent.Operation.READ, "entity.read",
                EntityItemAuditEvent.Operation.CREATE, "entity.create",
                EntityItemAuditEvent.Operation.UPDATE, "entity.update",
                EntityItemAuditEvent.Operation.DELETE, "entity.delete",
                AbstractEntityRelationAuditEvent.Operation.READ, "relation.read",
                AbstractEntityRelationAuditEvent.Operation.UPDATE, "relation.update",
                AbstractEntityRelationAuditEvent.Operation.DELETE, "relation.delete",
                EntityContentAuditEvent.Operation.READ, "content.read",
                EntityContentAuditEvent.Operation.UPDATE, "content.update",
                EntityContentAuditEvent.Operation.DELETE, "content.delete"
        );

        AuditEventOperationSerializer() {
            super(Enum.class, true);
        }

        @Override
        public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(Objects.requireNonNull(TYPES.get(value),
                    () -> "Operation '%s#%s' is not mapped to a string".formatted(value.getClass(), value.name())));
        }
    }
}
