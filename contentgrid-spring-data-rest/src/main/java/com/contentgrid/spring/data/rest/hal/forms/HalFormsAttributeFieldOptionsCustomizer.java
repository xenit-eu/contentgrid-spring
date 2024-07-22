package com.contentgrid.spring.data.rest.hal.forms;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.validation.AllowedValues;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

@RequiredArgsConstructor
public class HalFormsAttributeFieldOptionsCustomizer implements MediaTypeConfigurationCustomizer<HalFormsConfiguration> {

    @NonNull
    private final DomainTypeMapping domainTypeMapping;

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
        container.doWithProperties(property -> {
            property.findAnnotation(AllowedValues.class)
                    .map(AllowedValues::value)
                    .ifPresent(options -> configAtomic.updateAndGet(config -> {
                        return config.withOptions(domainType, property.getName(), metadata -> {
                            return HalFormsOptions.inline(options);
                        });
                    }));
        });
        return configAtomic.get();
    }
}
