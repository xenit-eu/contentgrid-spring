package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.BasicAuditEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

public class BasicAuditEventExtractor implements AuditEventExtractor {

    private static final Set<HandlerMethodMatcher> IGNORED_HANDLERS = Set.of(
            HandlerMethodMatcher.builder()
                    .className("org.springframework.data.rest.webmvc.halexplorer.HalExplorer")
                    .allMethods()
                    .build(),
            HandlerMethodMatcher.builder()
                    .className("com.contentgrid.spring.swagger.ui.SwaggerUIInitializerController")
                    .allMethods()
                    .build()
    );

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {
        var handler = context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (handler instanceof ResourceHttpRequestHandler) {
            // Don't log static resources
            return Optional.empty();
        }

        if (IGNORED_HANDLERS.stream().anyMatch(h -> h.matches(handler))) {
            return Optional.empty();
        }

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

    @Override
    public int getOrder() {
        // needs to run after all other handlers, so it can pick up the leftovers
        return Ordered.LOWEST_PRECEDENCE;
    }
}
