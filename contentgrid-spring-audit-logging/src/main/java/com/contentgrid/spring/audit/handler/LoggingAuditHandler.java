package com.contentgrid.spring.audit.handler;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityItemAuditEvent;
import com.contentgrid.spring.audit.event.AuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

@Slf4j
public class LoggingAuditHandler implements AuditEventHandler {

    @Override
    public void handle(AuditEvent auditEvent) {
        var auditLogBuilder = log.atLevel(Level.INFO);

        StringBuilder logMessage = new StringBuilder("AUDIT");

        if(auditEvent instanceof AbstractEntityAuditEvent entityAuditEvent)  {
            auditLogBuilder = auditLogBuilder.addArgument(entityAuditEvent.getDomainType().getSimpleName())
                    .addKeyValue("domainType", entityAuditEvent.getDomainType().getSimpleName());
            logMessage.append(" - {}");
        }
        if (auditEvent instanceof AbstractEntityItemAuditEvent entityItemAuditEvent) {
            auditLogBuilder = auditLogBuilder.addArgument(entityItemAuditEvent.getId())
                    .addKeyValue("id", entityItemAuditEvent.getId());
            logMessage.append("({})");
        }

        logMessage.append(" - ");

        if(auditEvent instanceof EntitySearchAuditEvent searchAuditEvent) {
            logMessage.append("search: {}");
            auditLogBuilder = auditLogBuilder.addArgument(searchAuditEvent.getQueryParameters())
                    .addKeyValue("query", searchAuditEvent.getQueryParameters());
        } else if(auditEvent instanceof EntityItemAuditEvent itemAuditEvent) {
            logMessage.append("{}");
            auditLogBuilder = auditLogBuilder.addArgument(itemAuditEvent.getOperation().name().toLowerCase(Locale.ROOT))
                    .addKeyValue("operation", itemAuditEvent.getOperation());
        } else if(auditEvent instanceof EntityRelationAuditEvent relationAuditEvent) {
            logMessage.append("{} relation {}");
            auditLogBuilder = auditLogBuilder.addArgument(relationAuditEvent.getOperation().name().toLowerCase(Locale.ROOT))
                    .addKeyValue("operation", relationAuditEvent.getOperation())
                    .addArgument(relationAuditEvent.getRelationName())
                    .addKeyValue("relation", relationAuditEvent.getRelationName());
        } else {
            logMessage.append("{}");
            auditLogBuilder = auditLogBuilder.addArgument(auditEvent);
        }

        auditLogBuilder.log(logMessage.toString());
    }
}
