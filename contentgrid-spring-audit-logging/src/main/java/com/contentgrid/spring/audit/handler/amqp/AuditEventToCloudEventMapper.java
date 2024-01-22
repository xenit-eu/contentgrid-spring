package com.contentgrid.spring.audit.handler.amqp;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.handler.amqp.AuditEventToCloudEventMapper.AuditEventToCloudEventMapperBuilder;
import java.util.Objects;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

interface AuditEventToCloudEventMapper {

    Class<?> getType();

    boolean supports(Object instance);

    String toCloudEventType(AbstractAuditEvent auditEvent);

    String toCloudEventSubject(AbstractAuditEvent auditEvent);

    static <T extends AbstractAuditEvent> AuditEventToCloudEventMapperBuilder<T> build(Class<T> eventType) {
        return new AuditEventToCloudEventMapperImpl<>(eventType, null, null);

    }

    interface AuditEventToCloudEventMapperBuilder<T extends AbstractAuditEvent> {

        AuditEventToCloudEventMapperBuilder<T> cloudEventType(Function<T, String> getCloudEventType);

        default AuditEventToCloudEventMapperBuilder<T> cloudEventType(String eventType) {
            return cloudEventType(_unused -> eventType);
        }

        default AuditEventToCloudEventMapperBuilder<T> cloudEventType(String prefix, Function<T, String> mapper) {
            return cloudEventType(event -> prefix + "." + mapper.apply(event));
        }

        AuditEventToCloudEventMapperBuilder<T> cloudEventSubject(Function<T, String> getCloudEventType);

        AuditEventToCloudEventMapper build();
    }
}

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@With(value = AccessLevel.PRIVATE)
class AuditEventToCloudEventMapperImpl<T extends AbstractAuditEvent> implements AuditEventToCloudEventMapper,
        AuditEventToCloudEventMapperBuilder<T> {

    @NonNull
    @Getter
    private final Class<T> type;

    private final Function<T, String> cloudEventType;
    private final Function<T, String> cloudEventSubject;

    @Override
    public boolean supports(Object instance) {
        return type.isInstance(instance);
    }

    @Override
    public String toCloudEventType(AbstractAuditEvent auditEvent) {
        return cloudEventType.apply(type.cast(auditEvent));
    }

    @Override
    public String toCloudEventSubject(AbstractAuditEvent auditEvent) {
        return cloudEventSubject.apply(type.cast(auditEvent));
    }

    @Override
    public AuditEventToCloudEventMapperBuilder<T> cloudEventType(Function<T, String> getCloudEventType) {
        return withCloudEventType(getCloudEventType);
    }

    @Override
    public AuditEventToCloudEventMapperBuilder<T> cloudEventSubject(Function<T, String> getCloudEventType) {
        return withCloudEventSubject(getCloudEventType);
    }

    @Override
    public AuditEventToCloudEventMapper build() {
        Objects.requireNonNull(cloudEventType, "cloudEventType");
        Objects.requireNonNull(cloudEventSubject, "cloudEventSubject");
        return this;
    }
}