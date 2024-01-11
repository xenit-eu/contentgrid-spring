package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AuditEvent.AuditEventBuilder;
import java.util.Optional;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Extracts audit information from a {@link ServerRequestObservationContext}
 *
 * Audit information is recorded in instances of (sub-classes of)
 * {@link com.contentgrid.spring.audit.event.AbstractAuditEvent}, defining the exact kind of audit event, and the
 * information that belongs with it.
 * <p>
 * Multiple {@link AuditEventExtractor}s can contribute additional information to an audit event when they have
 * additional information to attach.
 */
public interface AuditEventExtractor {
    Optional<AuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context);
    AuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context, AuditEventBuilder<?, ?> eventBuilder);
}
