package com.contentgrid.spring.audit.handler;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;

public interface AuditEventHandler {

    void handle(AbstractAuditEvent auditEvent);
}
