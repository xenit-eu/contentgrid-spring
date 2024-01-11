package com.contentgrid.spring.audit.handler;

import com.contentgrid.spring.audit.event.AuditEvent;

public interface AuditEventHandler {
    void handle(AuditEvent auditEvent);
}
