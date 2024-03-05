package com.contentgrid.spring.data.rest.hal.forms;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.server.EntityLinks;

@RequiredArgsConstructor
public class HalFormsRelationFieldOptionsCustomizer implements MediaTypeConfigurationCustomizer<HalFormsConfiguration> {

    private final DomainTypeMapping domainTypeMapping;
    private final EntityLinks entityLinks;

    @Override
    public HalFormsConfiguration customize(HalFormsConfiguration configuration) {
        for (Class<?> domainType : domainTypeMapping) {
            var container = domainTypeMapping.forDomainType(domainType);
            configuration = customizeConfiguration(configuration, domainType, container);
        }
        return configuration;
    }

    private HalFormsConfiguration customizeConfiguration(HalFormsConfiguration configuration, Class<?> domainType,
            Container container) {
        var configAtomic = new AtomicReference<>(configuration);
        container.doWithAssociations(association -> {
            configAtomic.updateAndGet(
                    config -> config.withOptions(domainType, association.getName(), metadata -> {
                        var collectionLink = entityLinks.linkToCollectionResource(
                                association.getTypeInformation().getRequiredActualType().getType()).expand();

                        return HalFormsOptions.remote(collectionLink)
                                .withMinItems(association.isRequired() ? 1L : 0L)
                                .withMaxItems(association.getTypeInformation().isCollectionLike() ? null : 1L)
                                // This is a JSON pointer into the item
                                .withValueField("/_links/self/href");
                    }));
        });
        return configAtomic.get();
    }
}
