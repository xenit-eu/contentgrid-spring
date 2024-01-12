package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractEntityRelationAuditEvent.AbstractEntityRelationAuditEventBuilder;
import com.contentgrid.spring.audit.event.AuditEvent.AuditEventBuilder;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntityRelationItemAuditEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

public class EntityRelationEventExtractor implements AuditEventExtractor {

    private static final String[] RELATION_CONTROLLERS = {
            "org.springframework.data.rest.webmvc.RepositoryPropertyReferenceController",
            "org.springframework.data.rest.webmvc.DelegatingRepositoryPropertyReferenceController"
    };

    private static final Map<HandlerMethodMatcher, Operation> METHODS;

    static {
        var methods = new HashMap<HandlerMethodMatcher, Operation>();
        for (String controller : RELATION_CONTROLLERS) {
            methods.putAll(createMatchers(controller));
        }
        METHODS = Collections.unmodifiableMap(methods);
    }

    private static Map<HandlerMethodMatcher, Operation> createMatchers(String relationController) {
        return Map.of(
                HandlerMethodMatcher.builder()
                        .className(relationController)
                        .methodName("followPropertyReference")
                        .methodName("followPropertyReferenceCompact")
                        .build(), Operation.READ,
                HandlerMethodMatcher.builder()
                        .className(relationController)
                        .methodName("createPropertyReference")
                        .build(), Operation.UPDATE,
                HandlerMethodMatcher.builder()
                        .className(relationController)
                        .methodName("deletePropertyReference")
                        .methodName("deletePropertyReferenceId")
                        .build(), Operation.DELETE
        );
    }

    @Override
    public Optional<AuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {

        var handlerMethod = context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        return METHODS.entrySet().stream()
                .filter(entry -> entry.getKey().matches(handlerMethod))
                .findFirst()
                .map(Entry::getValue)
                .map(operation -> {
                            var templateVariables = (Map<String, String>) context.getCarrier()
                                    .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                            var propertyId = templateVariables.get("propertyId");
                            if (propertyId != null) {
                                return EntityRelationItemAuditEvent.builder()
                                        .relationId(propertyId)
                                        .operation(switch (operation) {
                                            case READ -> EntityRelationItemAuditEvent.Operation.READ;
                                            case DELETE -> EntityRelationItemAuditEvent.Operation.DELETE;
                                            default -> throw new IllegalArgumentException();
                                        });
                            }
                            return EntityRelationAuditEvent.builder()
                                    .operation(operation);
                        }
                );
    }

    @Override
    public AuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AuditEventBuilder<?, ?> eventBuilder) {
        if(eventBuilder instanceof AbstractEntityRelationAuditEventBuilder<?,?> entityRelationAuditEvent) {
            var templateVariables = (Map<String, String>)context.getCarrier().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            return entityRelationAuditEvent.relationName(templateVariables.get("property"));
        }
        return eventBuilder;
    }
}
