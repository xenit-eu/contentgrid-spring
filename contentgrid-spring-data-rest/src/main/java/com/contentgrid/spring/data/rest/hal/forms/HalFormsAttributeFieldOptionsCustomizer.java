package com.contentgrid.spring.data.rest.hal.forms;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.validation.AllowedValues;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

@RequiredArgsConstructor
public class HalFormsAttributeFieldOptionsCustomizer implements
        MediaTypeConfigurationCustomizer<HalFormsConfiguration> {

    @NonNull
    private final DomainTypeMapping domainTypeMapping;

    @NonNull
    private final CollectionFiltersMapping collectionFiltersMapping;

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
        Set<String> configuredProperties = new HashSet<>();
        container.doWithProperties(property -> {
            property.findAnnotation(AllowedValues.class)
                    .map(AllowedValues::value)
                    .ifPresent(options -> configAtomic.updateAndGet(config -> {
                        configuredProperties.add(property.getName());
                        return config.withOptions(domainType, property.getName(), metadata -> {
                            return HalFormsOptions.inline(options)
                                    .withMinItems(metadata.isRequired() ? 1L : 0L)
                                    .withMaxItems(property.getTypeInformation().isCollectionLike() ? null : 1L);
                        });
                    }));
        });
        collectionFiltersMapping.forDomainType(domainType)
                .filters()
                // Exclude properties that were already configured
                .filter(f -> !configuredProperties.contains(f.getFilterName()))
                .forEach(filter -> {
                    var allowedValues = filter.getAnnotatedElement().getAnnotation(AllowedValues.class);
                    if (allowedValues == null) {
                        return;
                    }
                    configAtomic.updateAndGet(config -> {
                        return config.withOptions(domainType, filter.getFilterName(), metadata -> {
                            return HalFormsOptions.inline(allowedValues.value())
                                    // For searching, always restrict possible options to one
                                    .withMaxItems(1L);
                        });
                    });
                });
        return configAtomic.get();
    }
}
