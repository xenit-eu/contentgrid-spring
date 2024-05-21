package com.contentgrid.spring.data.rest.mapping;

import com.contentgrid.spring.data.querydsl.mapping.ContentGridCollectionFilterMappingConfiguration;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedContainer;
import com.contentgrid.spring.data.rest.mapping.persistent.ThroughAssociationsContainer;
import com.contentgrid.spring.data.rest.mapping.rest.DataRestBasedContainer;
import com.contentgrid.spring.data.rest.webmvc.DefaultDomainTypeToHalFormsPayloadMetadataConverter;
import com.contentgrid.spring.data.rest.webmvc.DomainTypeToHalFormsPayloadMetadataConverter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.ContentGridRestProperties;

@Configuration(proxyBeanMethods = false)
@Import(ContentGridCollectionFilterMappingConfiguration.class)
public class ContentGridDomainTypeMappingConfiguration {
    @Bean
    @PlainMapping
    DomainTypeMapping plainDomainTypeMapping(Repositories repositories) {
        return new DomainTypeMapping(repositories);
    }

    @Bean
    @PlainMapping(followingAssociations = true)
    DomainTypeMapping plainDomainTypeMappingFollowingAssociations(@PlainMapping DomainTypeMapping domainTypeMapping, Repositories repositories) {
        return domainTypeMapping.wrapWith(c -> new ThroughAssociationsContainer(c, repositories, Integer.MAX_VALUE));
    }

    @Bean
    @FormMapping
    DomainTypeMapping halFormsFormMappingDomainTypeMapping(@PlainMapping DomainTypeMapping plainMapping) {
        return plainMapping.wrapWith(c -> new JacksonBasedContainer(new DataRestBasedContainer(c)));
    }

    @Bean
    DomainTypeToHalFormsPayloadMetadataConverter defaultDomainTypeToHalFormsPayloadMetadataConverter(
            @FormMapping DomainTypeMapping formDomainTypeMapping,
            CollectionFiltersMapping collectionFiltersMapping,
            ContentGridRestProperties contentGridRestProperties
    ) {
        return new DefaultDomainTypeToHalFormsPayloadMetadataConverter(
                formDomainTypeMapping,
                collectionFiltersMapping,
                contentGridRestProperties.isUseMultipartHalForms()
        );
    }



}
