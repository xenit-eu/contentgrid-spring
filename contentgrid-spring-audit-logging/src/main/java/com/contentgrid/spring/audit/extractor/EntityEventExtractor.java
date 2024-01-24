package com.contentgrid.spring.audit.extractor;

import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.AbstractEntityAuditEvent.AbstractEntityAuditEventBuilder;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

@RequiredArgsConstructor
public class EntityEventExtractor implements AuditEventExtractor {

    private final RepositoryResourceMappings resourceMappings;

    @Override
    public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(ServerRequestObservationContext context) {
        return Optional.empty();
    }

    @Override
    public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
            AbstractAuditEventBuilder<?, ?> eventBuilder) {
        if (eventBuilder instanceof AbstractEntityAuditEventBuilder<?, ?> entityAuditEventBuilder) {
            Map<String, String> templateVariables = (Map<String, String>) context.getCarrier()
                    .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (templateVariables != null) {
                for (ResourceMetadata resourceMapping : resourceMappings) {
                    if (resourceMapping.isExported() && resourceMapping.getPath()
                            .matches(templateVariables.get("repository"))) {
                        return entityAuditEventBuilder
                                .domainType(resourceMapping.getDomainType());
                    }
                }
            }
        }
        return eventBuilder;
    }
}
