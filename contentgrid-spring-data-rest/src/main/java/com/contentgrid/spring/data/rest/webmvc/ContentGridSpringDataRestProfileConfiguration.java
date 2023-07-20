package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.webmvc.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.webmvc.mapping.jackson.JacksonBasedContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestProfileConfiguration {
    @Bean
    DomainTypeMapping halFormsFormMappingDomainTypeMapping(Repositories repositories) {
        return new DomainTypeMapping(repositories, JacksonBasedContainer::new);
    }

    @Bean
    DomainTypeToHalFormsPayloadMetadataConverter DomainTypeToHalFormsPayloadMetadataConverter(
            DomainTypeMapping formDomainTypeMapping
    ) {
        return new DefaultDomainTypeToHalFormsPayloadMetadataConverter(
                formDomainTypeMapping
        );
    }

}
