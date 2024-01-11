package com.contentgrid.spring.audit;

import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityItemCreateIdExtractor;
import com.contentgrid.spring.audit.extractor.EntityItemEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityRelationEventExtractor;
import com.contentgrid.spring.audit.extractor.EntitySearchEventExtractor;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;

@Configuration(proxyBeanMethods = false)
public class ContentGridAuditEventConfiguration {

    @Bean
    AuditObservationHandler auditObservabilityHandler(List<AuditEventExtractor> auditEventExtractors,
            List<AuditEventHandler> auditEventHandlers) {
        return new AuditObservationHandler(auditEventExtractors, auditEventHandlers);
    }

    @Bean
    EntityEventExtractor entityEventExtractor(RepositoryResourceMappings resourceMappings) {
        return new EntityEventExtractor(resourceMappings);
    }

    @Bean
    EntitySearchEventExtractor entitySearchEventExtractor() {
        return new EntitySearchEventExtractor();
    }

    @Bean
    EntityItemEventExtractor entityItemEventExtractor() {
        return new EntityItemEventExtractor();
    }

    @Bean
    EntityItemCreateIdExtractor entityItemCreateEventExtractor(PersistentEntities persistentEntities) {
        return new EntityItemCreateIdExtractor(persistentEntities);
    }

    @Bean
    EntityRelationEventExtractor entityRelationEventExtractor() {
        return new EntityRelationEventExtractor();
    }
}
