package com.contentgrid.spring.audit.handler.amqp;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

@RequiredArgsConstructor
public class AuditEventMessageConverter implements MessageConverter {

    private final Jackson2JsonMessageConverter converter;

    private final MessageConverter delegate;

    public AuditEventMessageConverter(ObjectMapper objectMapper, MessageConverter delegate) {
        this(new Jackson2JsonMessageConverter(objectMapper, AbstractAuditEvent.class.getPackageName()), delegate);
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (object instanceof AbstractAuditEvent auditEvent) {
            var message = converter.toMessage(auditEvent, messageProperties);
            message.getMessageProperties().setHeader("contentgrid:type", "audit");
            return message;
        }
        return delegate.toMessage(object, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        if (Objects.equals(message.getMessageProperties().getHeader("contentgrid:type"), "audit")) {
            return converter.fromMessage(message);
        }

        return delegate.fromMessage(message);
    }
}
