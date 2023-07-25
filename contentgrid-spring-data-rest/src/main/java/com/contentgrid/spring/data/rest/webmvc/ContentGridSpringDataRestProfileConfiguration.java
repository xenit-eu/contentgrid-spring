package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.webmvc.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.webmvc.mapping.jackson.JacksonBasedContainer;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.server.EntityLinks;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestProfileConfiguration {
    @Bean
    HalFormsProfileController halFormsProfileController(RepositoryRestConfiguration repositoryRestConfiguration, EntityLinks entityLinks, DomainTypeToHalFormsPayloadMetadataConverter domainTypeToHalFormsPayloadMetadataConverter) {
        return new HalFormsProfileController(repositoryRestConfiguration, entityLinks, domainTypeToHalFormsPayloadMetadataConverter);
    }

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

    @Bean
    HalFormsConfiguration halFormsConfiguration(ObjectProvider<HalConfiguration> halConfiguration, DomainTypeMapping domainTypeMapping, EntityLinks entityLinks) {
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
