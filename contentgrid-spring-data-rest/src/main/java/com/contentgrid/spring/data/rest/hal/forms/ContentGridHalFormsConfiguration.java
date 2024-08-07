package com.contentgrid.spring.data.rest.hal.forms;

import com.contentgrid.spring.data.rest.mapping.ContentGridDomainTypeMappingConfiguration;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.FormMapping;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.server.EntityLinks;

@Configuration(proxyBeanMethods = false)
@Import(ContentGridDomainTypeMappingConfiguration.class)
public class ContentGridHalFormsConfiguration {

    @Bean
    MediaTypeConfigurationCustomizer<HalFormsConfiguration> contentGridHalFormsRelationFieldOptionsCustomizer(
            @FormMapping DomainTypeMapping domainTypeMapping,
            EntityLinks entityLinks
    ) {
        return new HalFormsRelationFieldOptionsCustomizer(domainTypeMapping, entityLinks);
    }

    @Bean
    MediaTypeConfigurationCustomizer<HalFormsConfiguration> contentGridHalFormsAttributeFieldOptionsCustomizer(
            @FormMapping DomainTypeMapping domainTypeMapping,
            CollectionFiltersMapping collectionFiltersMapping
    ) {
        return new HalFormsAttributeFieldOptionsCustomizer(domainTypeMapping, collectionFiltersMapping);
    }
}
