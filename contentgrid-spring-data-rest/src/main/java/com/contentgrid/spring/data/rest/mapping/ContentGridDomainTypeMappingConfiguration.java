package com.contentgrid.spring.data.rest.mapping;

import com.contentgrid.spring.data.querydsl.mapping.ContentGridCollectionFilterMappingConfiguration;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedContainer;
import com.contentgrid.spring.data.rest.webmvc.DefaultDomainTypeToHalFormsPayloadMetadataConverter;
import com.contentgrid.spring.data.rest.webmvc.DomainTypeToHalFormsPayloadMetadataConverter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;

@Configuration(proxyBeanMethods = false)
@Import(ContentGridCollectionFilterMappingConfiguration.class)
public class ContentGridDomainTypeMappingConfiguration {
    @Bean
    @FormMapping
    DomainTypeMapping halFormsFormMappingDomainTypeMapping(Repositories repositories) {
        return new DomainTypeMapping(repositories, JacksonBasedContainer::new);
    }

    @Bean
    DomainTypeToHalFormsPayloadMetadataConverter DomainTypeToHalFormsPayloadMetadataConverter(
            @FormMapping DomainTypeMapping formDomainTypeMapping,
            CollectionFiltersMapping collectionFiltersMapping
    ) {
        return new DefaultDomainTypeToHalFormsPayloadMetadataConverter(
                formDomainTypeMapping,
                collectionFiltersMapping
        );
    }



}
