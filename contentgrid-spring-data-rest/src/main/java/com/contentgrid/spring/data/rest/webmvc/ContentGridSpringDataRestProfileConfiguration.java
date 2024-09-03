package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.mapping.ContentGridDomainTypeMappingConfiguration;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;

@Configuration(proxyBeanMethods = false)
@Import(ContentGridDomainTypeMappingConfiguration.class)
public class ContentGridSpringDataRestProfileConfiguration {

    @Bean
    HalFormsProfileController halFormsProfileController(
            RepositoryRestConfiguration repositoryRestConfiguration,
            EntityLinks entityLinks,
            DomainTypeToHalFormsPayloadMetadataConverter domainTypeToHalFormsPayloadMetadataConverter,
            @Qualifier("halFormsJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter messageConverter
    ) {
        var objectMapper = messageConverter.getObjectMapper().copy();
        return new HalFormsProfileController(repositoryRestConfiguration, entityLinks,
                domainTypeToHalFormsPayloadMetadataConverter, objectMapper);
    }


    @Bean
    DomainTypeToHalFormsPayloadMetadataConverter defaultDomainTypeToHalFormsPayloadMetadataConverter(
            Collection<HalFormsPayloadMetadataContributor> contributors
    ) {
        return new DefaultDomainTypeToHalFormsPayloadMetadataConverter(
                contributors
        );
    }

}
