package com.contentgrid.spring.audit;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@RequiredArgsConstructor
public class AuditObservationHandler implements ObservationHandler<ServerRequestObservationContext> {

    private final List<AuditEventExtractor> auditEventExtractors;
    private final List<AuditEventHandler> auditEventHandlers;

    @Override
    public boolean supportsContext(Context context) {
        return context instanceof ServerRequestObservationContext;
    }

    @Override
    public void onStop(ServerRequestObservationContext context) {
        var event = createAuditEvent(context);
        if (event != null) {
            for (AuditEventHandler auditEventHandler : auditEventHandlers) {
                auditEventHandler.handle(event);
            }
        }
    }

    private AbstractAuditEvent createAuditEvent(ServerRequestObservationContext context) {
        var maybeEventBuilder = auditEventExtractors.stream()
                .flatMap(e -> e.createEventBuilder(context).stream())
                .findFirst();

        if (maybeEventBuilder.isEmpty()) {
            return null;
        }

        var eventBuilder = maybeEventBuilder.get();
        for (AuditEventExtractor tagExtractor : auditEventExtractors) {
            eventBuilder = tagExtractor.enhance(context, eventBuilder);
        }

        return eventBuilder.build();
    }
}
