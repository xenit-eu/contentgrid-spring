package com.contentgrid.spring.audit.handler.messaging;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@RequiredArgsConstructor
public class AuditEventMessageConverter implements MessageConverter {

    private final MappingJackson2MessageConverter converter;

    public AuditEventMessageConverter(ObjectMapper objectMapper) {
        this(createConverter(objectMapper));
    }

    private static MappingJackson2MessageConverter createConverter(ObjectMapper objectMapper) {
        var converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        if (AbstractAuditEvent.class.isAssignableFrom(targetClass)) {
            return converter.fromMessage(message, targetClass);
        }
        return null;
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        if (payload instanceof AbstractAuditEvent auditEvent) {
            return converter.toMessage(auditEvent, headers);
        }
        return null;
    }
}
