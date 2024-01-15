package com.contentgrid.spring.audit.handler;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityAuditEvent;
import com.contentgrid.spring.audit.event.AbstractEntityItemAuditEvent;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Slf4j
public class LoggingAuditHandler implements AuditEventHandler {

    @Override
    public void handle(AbstractAuditEvent auditEvent) {
        var auditLogBuilder = log.atLevel(Level.INFO);

        StringBuilder logMessage = new StringBuilder("{} {} -> {}");

        auditLogBuilder = auditLogBuilder.addArgument(auditEvent.getRequestMethod())
                .addKeyValue("http.method", auditEvent.getRequestMethod())
                .addArgument(auditEvent.getRequestUri())
                .addKeyValue("http.uri", auditEvent.getRequestUri())
                .addArgument(auditEvent.getResponseStatus())
                .addKeyValue("http.status", auditEvent.getResponseStatus())
        ;
        var httpStatus = HttpStatus.resolve(auditEvent.getResponseStatus());
        if (httpStatus != null) {
            logMessage.append(" {}");
            auditLogBuilder = auditLogBuilder.addArgument(httpStatus.getReasonPhrase());
        }

        if (auditEvent.getResponseLocation() != null) {
            logMessage.append(" {}");
            auditLogBuilder = auditLogBuilder.addArgument(auditEvent.getResponseLocation())
                    .addKeyValue("responseLocation", auditEvent.getResponseLocation());
        }

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

        if (auditEvent instanceof EntitySearchAuditEvent searchAuditEvent) {
            logMessage.append(" - search: {}");
            auditLogBuilder = auditLogBuilder.addArgument(searchAuditEvent.getQueryParameters())
                    .addKeyValue("query", searchAuditEvent.getQueryParameters());
        } else if (auditEvent instanceof EntityItemAuditEvent itemAuditEvent) {
            logMessage.append(" - {}");
            auditLogBuilder = auditLogBuilder.addArgument(itemAuditEvent.getOperation().name().toLowerCase(Locale.ROOT))
                    .addKeyValue("operation", itemAuditEvent.getOperation());
        } else if (auditEvent instanceof EntityRelationAuditEvent relationAuditEvent) {
            logMessage.append(" - {} relation {}");
            auditLogBuilder = auditLogBuilder.addArgument(
                            relationAuditEvent.getOperation().name().toLowerCase(Locale.ROOT))
                    .addKeyValue("operation", relationAuditEvent.getOperation())
                    .addArgument(relationAuditEvent.getRelationName())
                    .addKeyValue("relation", relationAuditEvent.getRelationName());
        } else if (auditEvent instanceof EntityContentAuditEvent contentAuditEvent) {
            logMessage.append(" - {} content {}");
            auditLogBuilder = auditLogBuilder.addArgument(
                            contentAuditEvent.getOperation().name().toLowerCase(Locale.ROOT))
                    .addKeyValue("operation", contentAuditEvent.getOperation())
                    .addArgument(contentAuditEvent.getContentName())
                    .addKeyValue("content", contentAuditEvent.getContentName());
        } else if (auditEvent instanceof BasicAuditEvent) {
            // do nothing
        } else {
            logMessage.append(" - {}");
            auditLogBuilder = auditLogBuilder.addArgument(auditEvent);
        }

        auditLogBuilder.log(logMessage.toString());
    }
}
