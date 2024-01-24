package com.contentgrid.spring.audit.handler.messaging;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

@RequiredArgsConstructor
public class AuditEventToCloudEventMessageConverter implements MessageConverter {

    @NonNull
    private final MessageConverter delegate;

    @NonNull
    private final PojoCloudEventData.ToBytes<AbstractAuditEvent> mapper;

    @NonNull
    private final URI source;

    private static final Set<AuditEventToCloudEventMapper> TYPE_MAPPING = Set.of(
            AuditEventToCloudEventMapper.build(BasicAuditEvent.class)
                    .cloudEventType("cloud.contentgrid.audit.basic")
                    .cloudEventSubject(BasicAuditEvent::getRequestUri)
                    .build(),
            AuditEventToCloudEventMapper.build(EntityItemAuditEvent.class)
                    .cloudEventType("cloud.contentgrid.audit.entity",
                            e -> e.getOperation().name().toLowerCase(Locale.ROOT))
                    .cloudEventSubject(
                            e -> e.getOperation() == Operation.CREATE ? e.getResponseLocation() : e.getRequestUri())
                    .build(),
            AuditEventToCloudEventMapper.build(AbstractEntityRelationAuditEvent.class)
                    .cloudEventType("cloud.contentgrid.audit.entity.relation",
                            e -> e.getOperation().name().toLowerCase(Locale.ROOT))
                    .cloudEventSubject(AbstractAuditEvent::getRequestUri)
                    .build(),
            AuditEventToCloudEventMapper.build(EntityContentAuditEvent.class)
                    .cloudEventType("cloud.contentgrid.audit.entity.content",
                            e -> e.getOperation().name().toLowerCase(Locale.ROOT))
                    .cloudEventSubject(AbstractAuditEvent::getRequestUri)
                    .build(),
            AuditEventToCloudEventMapper.build(EntitySearchAuditEvent.class)
                    .cloudEventType("cloud.contentgrid.audit.entity.list")
                    .cloudEventSubject(AbstractAuditEvent::getRequestUri)
                    .build()
    );

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        // TODO: implement converting back from a message to audit event?
        return null;
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        if (payload instanceof AbstractAuditEvent auditEvent) {
            var typeMapper = TYPE_MAPPING.stream()
                    .filter(m -> m.supports(auditEvent))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unsupported audit event type %s".formatted(auditEvent)));

            var cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(source)
                    .withType(typeMapper.toCloudEventType(auditEvent))
                    .withSubject(typeMapper.toCloudEventSubject(auditEvent))
                    .withData(PojoCloudEventData.wrap(auditEvent, mapper))
                    .build();

            if (headers == null) {
                // This is required so the CloudEventMessageConverter doesn't try to put null in a hashmap
                headers = new MessageHeaders(null);
            }

            return delegate.toMessage(cloudEvent, headers);

        }
        return null;
    }
}
