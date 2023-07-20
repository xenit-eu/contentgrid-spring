package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.hateoas.spring.affordances.property.BasicPropertyMetadata;
import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.mediatype.html.HtmlInputType;

@RequiredArgsConstructor
public class DefaultDomainTypeToHalFormsPayloadMetadataConverter implements
        DomainTypeToHalFormsPayloadMetadataConverter {

    private final DomainTypeMapping formMapping;

    @Override
    public PayloadMetadata convertToCreatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType))
                .forEachOrdered(properties::add);
        return new PayloadMetadata() {
            @Override
            public Stream<PropertyMetadata> stream() {
                return properties.stream();
            }

            @Override
            public Class<?> getType() {
                return domainType;
            }
        };
    }

    @Override
    public PayloadMetadata convertToUpdatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType))
                .filter(property -> !Objects.equals(property.getInputType(), HtmlInputType.URL_VALUE))
                .forEachOrdered(properties::add);
        return new PayloadMetadata() {
            @Override
            public Stream<PropertyMetadata> stream() {
                return properties.stream();
    }

            @Override
            public Class<?> getType() {
                return domainType;
            }
        };
    }

    private Stream<PropertyMetadata> extractPropertyMetadataForForms(Container entity) {
        var output = Stream.<PropertyMetadata>builder();
        entity.doWithProperties(new RecursivePropertyConsumer(
                output,
                (property) -> new BasicPropertyMetadata(property.getName(), property.getTypeInformation().toTypeDescriptor().getResolvableType())
                        .withRequired(property.isRequired())
                        .withReadOnly(false),

                this::extractPropertyMetadataForForms
        ));

        entity.doWithAssociations(new RecursivePropertyConsumer(
                output,
                property -> new BasicPropertyMetadata(property.getName(), property.getTypeInformation().toTypeDescriptor().getResolvableType())
                        .withInputType(HtmlInputType.URL_VALUE)
                        .withRequired(property.isRequired())
                        .withReadOnly(false),
                this::extractPropertyMetadataForForms
        ));
        return output.build();
    }

    @RequiredArgsConstructor
    private static class RecursivePropertyConsumer implements Consumer<Property> {
        private final Consumer<PropertyMetadata> output;
        private final Function<Property, PropertyMetadata> factory;
        private final Function<Container, Stream<PropertyMetadata>> recursor;

        @Override
        public void accept(Property property) {
            if (property.isIgnored() || property.isReadOnly()) {
                return;
            }

            property.nestedContainer().ifPresentOrElse(container -> {
                recursor.apply(container)
                        .map(propertyMetadata -> new PrefixedPropertyMetadata(property.getName(), propertyMetadata))
                        .forEachOrdered(output);
            }, () -> {
                output.accept(factory.apply(property));
            });
        }
    }

    @RequiredArgsConstructor
    @ToString
    private static class PrefixedPropertyMetadata implements PropertyMetadata {

        private final String prefix;
        private final PropertyMetadata delegate;

        @Override
        public String getName() {
            return prefix + "." + delegate.getName();
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
