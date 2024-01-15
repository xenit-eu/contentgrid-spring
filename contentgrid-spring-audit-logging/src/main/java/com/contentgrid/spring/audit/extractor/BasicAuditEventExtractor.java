package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.http.server.observation.ServerRequestObservationContext;

public class BasicAuditEventExtractor implements AuditEventExtractor {

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {
        return Optional.of(BasicAuditEvent.builder());
    }

    @Override
    public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder) {
        var location = Optional.ofNullable(context.getResponse().getHeader("Location"))
                .map(URI::create)
                .map(uri -> {
                    try {
                        return new URI(
                                null,
                                null,
                                uri.getPath(),
                                uri.getQuery(),
                                uri.getFragment()
                        ).toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
        return eventBuilder
                .requestMethod(context.getCarrier().getMethod())
                .requestUri(context.getCarrier().getRequestURI())
                .responseStatus(context.getResponse().getStatus())
                .responseLocation(location.orElse(null));
    }
}
