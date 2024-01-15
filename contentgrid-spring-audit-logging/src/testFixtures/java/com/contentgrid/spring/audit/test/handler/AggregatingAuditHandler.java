package com.contentgrid.spring.audit.test.handler;


import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class AggregatingAuditHandler implements AuditEventHandler {
    @Getter
    private final List<AbstractAuditEvent> events = new ArrayList<>();

    @Override
    public void handle(AbstractAuditEvent auditEvent) {
        events.add(auditEvent);
    }
}
