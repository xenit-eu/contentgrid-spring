package com.contentgrid.spring.audit.handler.amqp;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

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
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (object instanceof AbstractAuditEvent auditEvent) {
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

            return delegate.toMessage(cloudEvent, messageProperties);

        }
        return delegate.toMessage(object, messageProperties);
    }

    @Override
    @SneakyThrows
    public Object fromMessage(Message message) throws MessageConversionException {
        return delegate.fromMessage(message);
    }
}
