package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import java.util.Optional;
import org.springframework.core.Ordered;
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
public interface AuditEventExtractor extends Ordered {

    /**
     * Creates a builder for the audit event
     * <p>
     * Only one {@link AbstractAuditEventBuilder} can be created for a request; the first extractor that returns a
     * builder will be used. Typically, the most precise builder is returned, with minimal information filled in.
     * Additional information is provided by the
     * {@link #enhance(ServerRequestObservationContext, AbstractAuditEventBuilder)} method
     *
     * @param context The server request to audit
     * @return If this request can be handled by this extractor, a filled {@link Optional}.
     */
    Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context);

    /**
     * Enhances an audit event builder with additional information
     *
     * Multiple extractors can contribute information into a single {@link AbstractAuditEventBuilder} that was created
     * for a request. Typically, a typecheck is done on the event builder, to see if this extractor can contribute
     * additional information
     *
     * @param context The server request to extract audit information from
     * @param eventBuilder The existing event builder (that was created by
     * {@link #createEventBuilder(ServerRequestObservationContext)})
     * @return The event builder (or a new instance in case the builder has to be replaced)
     */
    AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder);

    @Override
    default int getOrder() {
        return 0;
    }
}
