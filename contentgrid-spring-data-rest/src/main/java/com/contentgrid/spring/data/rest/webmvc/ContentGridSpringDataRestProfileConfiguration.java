package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.FormMapping;
import com.contentgrid.spring.data.rest.mapping.SearchMapping;
import com.contentgrid.spring.data.rest.mapping.collectionfilter.CollectionFilterBasedContainer;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedContainer;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestProfileConfiguration {
    @Bean
    HalFormsProfileController halFormsProfileController(
            RepositoryRestConfiguration repositoryRestConfiguration,
            EntityLinks entityLinks,
            DomainTypeToHalFormsPayloadMetadataConverter domainTypeToHalFormsPayloadMetadataConverter,
            @Qualifier("halFormsJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter messageConverter
    ) {
        var objectMapper = messageConverter.getObjectMapper().copy();
        return new HalFormsProfileController(repositoryRestConfiguration, entityLinks, domainTypeToHalFormsPayloadMetadataConverter, objectMapper);
    }

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

    @Bean
    HalFormsConfiguration halFormsConfiguration(ObjectProvider<HalConfiguration> halConfiguration, @FormMapping DomainTypeMapping domainTypeMapping, EntityLinks entityLinks) {
        var halFormsConfiguration = new AtomicReference<>(new HalFormsConfiguration(halConfiguration.getIfAvailable(HalConfiguration::new)));
        for (Class<?> domainType : domainTypeMapping) {
            var container = domainTypeMapping.forDomainType(domainType);

            container.doWithAssociations(property -> {
                halFormsConfiguration.updateAndGet(config -> config.withOptions(domainType, property.getName(), metadata -> {
                    var collectionLink = entityLinks.linkToCollectionResource(property.getTypeInformation().getRequiredActualType().getType()).expand();
                    return HalFormsOptions.remote(collectionLink)
                            .withMinItems(property.isRequired()?1L:0L)
                            .withMaxItems(property.getTypeInformation().isCollectionLike()?null:1L)
                            // This is a JSON pointer into the item
                            .withValueField("/_links/self/href");
                }));
            });
        }

        return halFormsConfiguration.get();
    }

}
