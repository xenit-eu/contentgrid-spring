package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent.EntityContentAuditEventBuilder;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent.Operation;
import internal.org.springframework.content.rest.io.AssociatedStoreResource;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

@RequiredArgsConstructor
public class EntityContentEventExtractor implements AuditEventExtractor {

    private static final String STORE_CONTROLLER = "internal.org.springframework.content.rest.controllers.StoreRestController";
    private final PersistentEntities entities;


    private static final Map<HandlerMethodMatcher, Operation> METHODS = Map.of(
            HandlerMethodMatcher.builder()
                    .className(STORE_CONTROLLER)
                    .methodName("getContent")
                    .build(), Operation.READ,
            HandlerMethodMatcher.builder()
                    .className(STORE_CONTROLLER)
                    .methodName("putContent")
                    .methodName("putMultipartContent")
                    .methodName("postContent")
                    .methodName("postMultipartContent")
                    .build(), Operation.UPDATE,
            HandlerMethodMatcher.builder()
                    .className(STORE_CONTROLLER)
                    .methodName("deleteContent")
                    .build(), Operation.DELETE
    );

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {
        var handlerMethod = context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return METHODS.entrySet().stream()
                .filter(e -> e.getKey().matches(handlerMethod))
                .findFirst()
                .map(Entry::getValue)
                .map(operation -> EntityContentAuditEvent.builder()
                        .operation(operation)
                );
    }

    @Override
    public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder) {
        if (eventBuilder instanceof EntityContentAuditEventBuilder<?, ?> contentAuditEventBuilder) {
            var springContentResource = context.getCarrier().getAttribute("SPRING_CONTENT_RESOURCE");
            if (springContentResource instanceof AssociatedStoreResource storeResource) {
                var domainType = storeResource.getStoreInfo().getDomainObjectClass();
                var idProperty = entities.getRequiredPersistentEntity(domainType).getRequiredIdProperty();
                var entityId = idProperty.getAccessorForOwner(storeResource.getAssociation())
                        .getProperty(idProperty);
                contentAuditEventBuilder
                        .domainType(domainType)
                        .contentName(storeResource.getPropertyPath().getName())
                        .id(entityId.toString());
            }
        }
        return eventBuilder;
    }
}
