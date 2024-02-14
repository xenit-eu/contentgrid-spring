package com.contentgrid.spring.audit;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@RequiredArgsConstructor
@Slf4j
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
                try {
                    auditEventHandler.handle(event);
                } catch (Exception ex) {
                    log.error("Audit event handler {} failed to process event {}", auditEventHandler, event, ex);
                }
            }
        }
    }

    private AbstractAuditEvent createAuditEvent(ServerRequestObservationContext context) {
        var maybeEventBuilder = auditEventExtractors.stream()
                .flatMap(eventExtractor -> {
                    try {
                        return eventExtractor.createEventBuilder(context).stream();
                    } catch (Exception ex) {
                        log.error("Audit event extractor {} failed to process context", eventExtractor, ex);
                        return Stream.empty();
                    }
                })
                .findFirst();

        if (maybeEventBuilder.isEmpty()) {
            return null;
        }

        var eventBuilder = maybeEventBuilder.get();
        for (AuditEventExtractor tagExtractor : auditEventExtractors) {
            try {
                var newEventBuilder = tagExtractor.enhance(context, eventBuilder);
                if (newEventBuilder != null) {
                    eventBuilder = newEventBuilder;
                } else {
                    log.error("Audit event extractor {} failed to enhance event {}: returned a null eventBuilder",
                            tagExtractor, eventBuilder);
                }
            } catch (Exception ex) {
                log.error("Audit event extractor {} failed to enhance event {}", tagExtractor, eventBuilder, ex);
            }
        }

        return eventBuilder.build();
    }
}
