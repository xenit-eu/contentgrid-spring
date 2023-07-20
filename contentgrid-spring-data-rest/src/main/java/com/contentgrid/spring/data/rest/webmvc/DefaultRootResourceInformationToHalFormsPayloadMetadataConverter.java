package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.hateoas.spring.affordances.property.BasicPropertyMetadata;
import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.jackson.JacksonMappingWrapper;
import com.contentgrid.spring.data.rest.webmvc.mapping.persistent.PersistentEntityContainer;
import com.contentgrid.spring.data.rest.webmvc.mapping.wrapper.MappingWrapper;
import com.contentgrid.spring.data.rest.webmvc.mapping.wrapper.WrappingContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.ResolvableType;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;

public class DefaultRootResourceInformationToHalFormsPayloadMetadataConverter implements
        RootResourceInformationToHalFormsPayloadMetadataConverter {

    private static final MappingWrapper MAPPING_WRAPPER = new JacksonMappingWrapper();

    @Override
    public PayloadMetadata convertToCreatePayloadMetadata(RootResourceInformation resourceInformation) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadata(new WrappingContainer(new PersistentEntityContainer(resourceInformation.getPersistentEntity()), MAPPING_WRAPPER))
                .forEachOrdered(properties::add);
        return new PayloadMetadata() {
            @Override
            public Stream<PropertyMetadata> stream() {
                return properties.stream();
            }

            @Override
            public Class<?> getType() {
                return resourceInformation.getDomainType();
            }
        };
    }

    @Override
    public PayloadMetadata convertToUpdatePayloadMetadata(RootResourceInformation resourceInformation) {
        return convertToCreatePayloadMetadata(resourceInformation);
    }

    private Stream<PropertyMetadata> extractPropertyMetadata(Container entity) {
        var output = Stream.<PropertyMetadata>builder();
        entity.doWithAll(property -> {
            if(property.isIgnored() || property.isReadOnly()) {
                return;
            }

            property.nestedContainer().ifPresentOrElse(container -> {
                extractPropertyMetadata(container)
                        .map(propertyMetadata -> new PrefixedPropertyMetadata(property.getName(), propertyMetadata))
                        .forEachOrdered(output::add);
            }, () -> {
                output.add(new BasicPropertyMetadata(property.getName(),
                        property.getTypeInformation().toTypeDescriptor().getResolvableType())
                        .withRequired(property.isRequired())
                        .withReadOnly(false)
                );
            });
        });
        return output.build();
    }

    @RequiredArgsConstructor
    @ToString
    private static class PrefixedPropertyMetadata implements PropertyMetadata {
        private final String prefix;
        private final PropertyMetadata delegate;

        @Override
        public String getName() {
            return prefix+"."+delegate.getName();
        }

        @Override
        public boolean isRequired() {
            return delegate.isRequired();
        }

        @Override
        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        @Override
        public Optional<String> getPattern() {
            return delegate.getPattern();
        }

        @Override
        public ResolvableType getType() {
            return delegate.getType();
        }

        @Override
        public String getInputType() {
            return delegate.getInputType();
        }
    }

}
