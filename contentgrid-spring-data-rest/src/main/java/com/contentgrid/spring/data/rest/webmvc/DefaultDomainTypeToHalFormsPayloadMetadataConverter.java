package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.hateoas.AffordanceModel.InputPayloadMetadata;
import org.springframework.hateoas.AffordanceModel.Named;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.mediatype.InputTypeFactory;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class DefaultDomainTypeToHalFormsPayloadMetadataConverter implements
        DomainTypeToHalFormsPayloadMetadataConverter {

    private final DomainTypeMapping formMapping;
    private final CollectionFiltersMapping searchMapping;

    @Override
    public PayloadMetadata convertToCreatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType))
                .forEachOrdered(properties::add);
        return new ClassnameI18nedPayloadMetadata(domainType, properties);
    }

    @Override
    public PayloadMetadata convertToUpdatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType))
                .filter(property -> !Objects.equals(property.getInputType(), HtmlInputType.URL_VALUE))
                .forEachOrdered(properties::add);
        return new ClassnameI18nedPayloadMetadata(domainType, properties);
    }

    @Override
    public PayloadMetadata convertToSearchPayloadMetadata(Class<?> domainType) {
        var properties = searchMapping.forDomainType(domainType)
                .filters()
                .filter(CollectionFilter::isDocumented)
                .<PropertyMetadata>map(filter -> new BasicPropertyMetadata(
                                filter.getFilterName(),
                                ResolvableType.forClass(filter.getPath().getType())
                        )
                        .withReadOnly(false)
                ).toList();

        return new ClassnameI18nedPayloadMetadata(domainType, properties);
    }

    private Stream<PropertyMetadata> extractPropertyMetadataForForms(Container entity) {
        var output = Stream.<PropertyMetadata>builder();
        entity.doWithProperties(new RecursivePropertyConsumer(
                output,
                (property) -> new BasicPropertyMetadata(property.getName(),
                        property.getTypeInformation().toTypeDescriptor().getResolvableType())
                        .withRequired(property.isRequired())
                        .withReadOnly(false),

                this::extractPropertyMetadataForForms
        ));

        entity.doWithAssociations(new RecursivePropertyConsumer(
                output,
                property -> new BasicPropertyMetadata(property.getName(),
                        property.getTypeInformation().toTypeDescriptor().getResolvableType())
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

    @Value
    @With
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BasicPropertyMetadata implements PropertyMetadata {
        private static final InputTypeFactory INPUT_TYPE_FACTORY;

        static {
            INPUT_TYPE_FACTORY = SpringFactoriesLoader.loadFactories(InputTypeFactory.class, BasicPropertyMetadata.class.getClassLoader()).get(0);
        }

        @NonNull
        String name;
        boolean required;
        boolean readOnly;
        @NonNull
        ResolvableType type;
        @Nullable
        String inputType;

        /**
         * @param propertyName The name of the property
         * @param type The type of the property
         */
        public BasicPropertyMetadata(String propertyName, ResolvableType type) {
            this(propertyName, false, false, type, INPUT_TYPE_FACTORY.getInputType(type.resolve(Object.class)));
        }

        @Override
        public Optional<String> getPattern() {
            return Optional.empty();
        }
    }


    @AllArgsConstructor
    @RequiredArgsConstructor
    static class ClassnameI18nedPayloadMetadata implements InputPayloadMetadata {
        private final Class<?> domainType;
        private final Collection<PropertyMetadata> properties;
        @With
        private List<MediaType> mediaTypes = Collections.emptyList();

        @Override
        public <T extends Named> T customize(T target, Function<PropertyMetadata, T> customizer) {
            return properties.stream()
                    .filter(propMeta -> propMeta.getName().equals(target.getName()))
                    .findAny()
                    .map(customizer)
                    .orElse(target);
        }

        @Override
        public List<String> getI18nCodes() {
            return List.of(domainType.getName());
        }

        @Override
        public List<MediaType> getMediaTypes() {
            return this.mediaTypes;
        }

        @Override
        public Stream<PropertyMetadata> stream() {
            return properties.stream();
        }

        @Override
        public Class<?> getType() {
            return this.domainType;
        }
    }

}
