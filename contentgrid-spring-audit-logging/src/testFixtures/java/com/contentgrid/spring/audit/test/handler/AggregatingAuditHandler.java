package com.contentgrid.spring.audit.test.handler;


import com.contentgrid.spring.audit.event.AuditEvent;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class AggregatingAuditHandler implements AuditEventHandler {
    @Getter
    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void handle(AuditEvent auditEvent) {
        events.add(auditEvent);
    }
}
