package com.contentgrid.spring.audit.handler.messaging;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import java.io.IOException;

public class Jackson2AuditMessagingModule extends SimpleModule {

    public Jackson2AuditMessagingModule() {
        super("contentgrid-audit-messaging",
                new Version(1, 0, 0, null, "com.contentgrid.spring", "contentgrid-spring-audit-logging"));

        setMixInAnnotation(AbstractAuditEvent.class, AbstractAuditEventMixin.class);
    }

    abstract static class AbstractAuditEventMixin {

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

        @JsonIgnore
        public abstract Object getOperation();
    }

    static class EntityTypeToNameSerializer extends ClassSerializer {

        @Override
        public void serialize(Class<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getSimpleName());
        }
    }
}
