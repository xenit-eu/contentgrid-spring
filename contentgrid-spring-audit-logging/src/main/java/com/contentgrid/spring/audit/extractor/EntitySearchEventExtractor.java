package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent.EntitySearchAuditEventBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

@RequiredArgsConstructor
public class EntitySearchEventExtractor implements AuditEventExtractor {

    private static final HandlerMethodMatcher SEARCH_METHODS = HandlerMethodMatcher.builder()
            .className("org.springframework.data.rest.webmvc.RepositoryEntityController")
            .methodName("getCollectionResource")
            .methodName("getCollectionResourceCompact")
            .build();

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {
        var handler = context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (SEARCH_METHODS.matches(handler)) {
            return Optional.of(EntitySearchAuditEvent.builder());
        }

        return Optional.empty();
    }

    @Override
    public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder) {

        if (eventBuilder instanceof EntitySearchAuditEventBuilder<?, ?> searchAuditEventBuilder) {
            var parameters = context.getCarrier().getParameterMap()
                    .entrySet()
                    .stream()
                    .map(e -> Map.entry(e.getKey(), Arrays.asList(e.getValue())))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            return searchAuditEventBuilder.queryParameters(parameters);
        }

        return eventBuilder;
    }

}
