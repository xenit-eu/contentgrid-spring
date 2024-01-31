package com.contentgrid.spring.audit.handler.messaging;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.core.MessageSendingOperations;

@RequiredArgsConstructor
public class MessageSendingAuditHandler implements AuditEventHandler {

    @NonNull
    private final MessageSendingOperations<String> messageSendingOperations;

    @Nullable
    private final String destination;

    public void handle(AbstractAuditEvent auditEvent) {
        if (destination == null) {
            messageSendingOperations.convertAndSend(auditEvent);
        } else {
            messageSendingOperations.convertAndSend(destination, auditEvent);
        }
    }
}
