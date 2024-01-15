package com.contentgrid.spring.audit;

import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import com.contentgrid.spring.audit.extractor.BasicAuditEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityContentEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityItemCreateIdExtractor;
import com.contentgrid.spring.audit.extractor.EntityItemEventExtractor;
import com.contentgrid.spring.audit.extractor.EntityRelationEventExtractor;
import com.contentgrid.spring.audit.extractor.EntitySearchEventExtractor;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
    @Order(Ordered.LOWEST_PRECEDENCE)
        // must be after all others
    BasicAuditEventExtractor basicAuditEventExtractor() {
        return new BasicAuditEventExtractor();
    }

    @Bean
    @Order(0)
    EntityEventExtractor entityEventExtractor(RepositoryResourceMappings resourceMappings) {
        return new EntityEventExtractor(resourceMappings);
    }

    @Bean
    @Order(0)
    EntitySearchEventExtractor entitySearchEventExtractor() {
        return new EntitySearchEventExtractor();
    }

    @Bean
    @Order(0)
    EntityItemEventExtractor entityItemEventExtractor() {
        return new EntityItemEventExtractor();
    }

    @Bean
    @Order(0)
    EntityItemCreateIdExtractor entityItemCreateEventExtractor(PersistentEntities persistentEntities) {
        return new EntityItemCreateIdExtractor(persistentEntities);
    }

    @Bean
    @Order(0)
    EntityRelationEventExtractor entityRelationEventExtractor() {
        return new EntityRelationEventExtractor();
    }

    @Bean
    @Order(0)
    EntityContentEventExtractor entityContentEventExtractor(PersistentEntities persistentEntities) {
        return new EntityContentEventExtractor(persistentEntities);
    }
}
