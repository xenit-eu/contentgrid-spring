package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.AbstractEntityItemAuditEvent.AbstractEntityItemAuditEventBuilder;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

public class EntityItemEventExtractor implements AuditEventExtractor {

    private static final String REPOSITORY_ENTITY_CONTROLLER = "org.springframework.data.rest.webmvc.RepositoryEntityController";

    private static final Map<HandlerMethodMatcher, EntityItemAuditEvent.Operation> METHODS = Map.of(
            HandlerMethodMatcher.builder()
                    .className(REPOSITORY_ENTITY_CONTROLLER)
                    .methodName("headForItemResource")
                    .methodName("getItemResource")
                    .build(), Operation.READ,
            HandlerMethodMatcher.builder()
                    .className(REPOSITORY_ENTITY_CONTROLLER)
                    .methodName("postCollectionResource")
                    .build(), Operation.CREATE,
            HandlerMethodMatcher.builder()
                    .className(REPOSITORY_ENTITY_CONTROLLER)
                    .methodName("putItemResource")
                    .methodName("patchItemResource")
                    .build(), Operation.UPDATE,
            HandlerMethodMatcher.builder()
                    .className(REPOSITORY_ENTITY_CONTROLLER)
                    .methodName("deleteItemResource")
                    .build(), Operation.DELETE
    );

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {

        var handlerMethod = context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        return METHODS.entrySet().stream()
                .filter(entry -> entry.getKey().matches(handlerMethod))
                .findFirst()
                .map(Entry::getValue)
                .map(operation -> EntityItemAuditEvent.builder()
                        .operation(operation)
                );
    }

    @Override
    public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder) {
        if (eventBuilder instanceof AbstractEntityItemAuditEventBuilder<?, ?> entityItemAuditEventBuilder) {
            var templateVariables = (Map<String, String>) context.getCarrier()
                    .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            Optional.ofNullable(templateVariables.get("id"))
                    .ifPresent(entityItemAuditEventBuilder::id);
        }
        return eventBuilder;
    }
}
