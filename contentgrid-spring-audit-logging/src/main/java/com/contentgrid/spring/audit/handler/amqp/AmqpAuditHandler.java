package com.contentgrid.spring.audit.handler.amqp;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class AmqpAuditHandler implements AuditEventHandler {

    @NonNull
    private final AmqpTemplate amqpTemplate;

    @Nullable
    @With
    private final String exchange;

    @Nullable
    @With
    private final String routingKey;

    public AmqpAuditHandler(AmqpTemplate amqpTemplate) {
        this(amqpTemplate, null, null);
    }

    @Override
    public void handle(AbstractAuditEvent auditEvent) {
        amqpTemplate.convertAndSend(exchange, routingKey, auditEvent);
    }
}
