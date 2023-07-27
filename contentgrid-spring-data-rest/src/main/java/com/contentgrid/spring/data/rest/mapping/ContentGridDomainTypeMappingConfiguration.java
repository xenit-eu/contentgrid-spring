package com.contentgrid.spring.data.rest.mapping;

import com.contentgrid.spring.data.rest.mapping.collectionfilter.CollectionFilterBasedContainer;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedContainer;
import com.contentgrid.spring.data.rest.webmvc.DefaultDomainTypeToHalFormsPayloadMetadataConverter;
import com.contentgrid.spring.data.rest.webmvc.DomainTypeToHalFormsPayloadMetadataConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;

@Configuration(proxyBeanMethods = false)
public class ContentGridDomainTypeMappingConfiguration {
    @Bean
    @FormMapping
    DomainTypeMapping halFormsFormMappingDomainTypeMapping(Repositories repositories) {
        return new DomainTypeMapping(repositories, JacksonBasedContainer::new);
    }

    @Bean
    @SearchMapping
    DomainTypeMapping halFormsSearchMappingDomainTypeMapping(Repositories repositories) {
        return new DomainTypeMapping(repositories, (container) -> new CollectionFilterBasedContainer(container, 2));
    }

    @Bean
    DomainTypeToHalFormsPayloadMetadataConverter DomainTypeToHalFormsPayloadMetadataConverter(
            @FormMapping DomainTypeMapping formDomainTypeMapping,
            @SearchMapping DomainTypeMapping searchDomainTypeMapping
    ) {
        return new DefaultDomainTypeToHalFormsPayloadMetadataConverter(
                formDomainTypeMapping,
                searchDomainTypeMapping
        );
    }

}
