package com.contentgrid.spring.audit;

import com.contentgrid.spring.audit.event.AuditEvent;
import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Slf4j
@RequiredArgsConstructor
public class AuditObservationHandler implements ObservationHandler<ServerRequestObservationContext> {

    private final List<AuditEventExtractor> auditEventExtractors;

    @Override
    public boolean supportsContext(Context context) {
        return context instanceof ServerRequestObservationContext;
    }

    @Override
    public void onStop(ServerRequestObservationContext context) {
        var event = createAuditEvent(context);
        if(event != null) {
            log.info("AUDIT: {}", event);
        }
    }

    private AuditEvent createAuditEvent(ServerRequestObservationContext context) {
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
