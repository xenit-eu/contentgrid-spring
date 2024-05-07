package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.apache.commons.lang.CharUtils;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.mappingcontext.MappingContext;
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
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class DefaultDomainTypeToHalFormsPayloadMetadataConverter implements
        DomainTypeToHalFormsPayloadMetadataConverter {

    private final DomainTypeMapping formMapping;
    private final CollectionFiltersMapping searchMapping;
    // Optional means it only gets autowired if available
    private final Optional<MappingContext> contentMappingContext;

    @Override
    public PayloadMetadata convertToCreatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType),
                domainType,
                "", // path prefix starts empty
                (prop) -> (!prop.isReadOnly() && !prop.isIgnored() && prop.findAnnotation(MimeType.class).isEmpty() && prop.findAnnotation(
                        OriginalFileName.class).isEmpty()) || prop.findAnnotation(ContentId.class).isPresent(),
                this::propertyToMetadataForCreateForm
        ).forEachOrdered(properties::add);
        return new ClassnameI18nedPayloadMetadata(domainType, properties);
    }

    @Override
    public PayloadMetadata convertToUpdatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();
        extractPropertyMetadataForForms(formMapping.forDomainType(domainType),
                domainType,
                "", // path prefix starts empty
                (prop) -> (!prop.isReadOnly() && !prop.isIgnored()),
                this::propertyToMetadataForUpdateForm
        )
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

    private Stream<PropertyMetadata> extractPropertyMetadataForForms(Container entity, Class<?> domainType,
            String pathPrefix, Predicate<Property> filter, PropertyMetadataFactory attributeFactory) {
        var output = Stream.<PropertyMetadata>builder();
        entity.doWithProperties(new RecursivePropertyConsumer(
                output,
                attributeFactory,
                filter,
                domainType,
                pathPrefix
        ));

        entity.doWithAssociations(new RecursivePropertyConsumer(
                output,
                (property, a, b) -> new BasicPropertyMetadata(property.getName(),
                        property.getTypeInformation().toTypeDescriptor().getResolvableType())
                        .withInputType(HtmlInputType.URL_VALUE)
                        .withRequired(property.isRequired())
                        .withReadOnly(false),
                filter,
                domainType,
                pathPrefix
        ));
        return output.build();
    }

    private PropertyMetadata propertyToMetadataForCreateForm(Property property, Class<?> domainClass, String path) {

        var contentPropertyKey = contentMappingContext.flatMap(context -> context.getContentPropertyMap(domainClass)
                .entrySet()
                .stream()
                .filter(entry -> pathMatches(entry.getValue().getContentIdPropertyPath(), path))
                .findFirst()
                .map(Entry::getKey));

        if (contentPropertyKey.isPresent()) {
            return new BasicPropertyMetadata(contentPropertyKey.get(), ResolvableType.forClass(File.class))
                    .withRequired(property.isRequired())
                    .withReadOnly(false);
        }

        return new BasicPropertyMetadata(path,
                property.getTypeInformation().toTypeDescriptor().getResolvableType())
                .withRequired(property.isRequired())
                .withReadOnly(false);
    }

    private PropertyMetadata propertyToMetadataForUpdateForm(Property property, Class<?> domainClass, String path) {
        return new BasicPropertyMetadata(path,
                property.getTypeInformation().toTypeDescriptor().getResolvableType())
                .withRequired(property.isRequired())
                .withReadOnly(false);
    }


    @RequiredArgsConstructor
    private class RecursivePropertyConsumer implements Consumer<Property> {
        private final Consumer<PropertyMetadata> output;
        private final PropertyMetadataFactory factory;
        private final Predicate<Property> filter;

        private final Class<?> domainType;
        private final String pathPrefix;


        @Override
        public void accept(Property property) {
            if (!filter.test(property)) {
                return;
            }

            String path = pathPrefix.length() == 0
                    ? property.getName()
                    : pathPrefix + "." + property.getName();

            property.nestedContainer().ifPresentOrElse(container -> {
                extractPropertyMetadataForForms(container, domainType, path, filter, factory)
                        .forEachOrdered(output);
            }, () -> {
                output.accept(factory.build(property, domainType, path));
            });
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

    @FunctionalInterface
    static interface PropertyMetadataFactory {
        PropertyMetadata build(Property property, Class<?> domainClass, String path);
    }

    private static boolean pathMatches(String contentIdPropertyPath, String givenPath) {
        return Objects.equals(
                // names that are reserved keywords, like "public", have a leading _ in their java property
                StringUtils.trimLeadingCharacter(contentIdPropertyPath, '_'),
                // to match the java field name, we must convert the snake_case from the given path to camelCase
                underscoreToCamelCase(givenPath)
        );
    }

    private static String underscoreToCamelCase(String underscored) {
        int index = underscored.indexOf('_');
        if (index == -1) {
            return underscored;
        }

        int from = 0;
        StringBuilder camel = new StringBuilder();
        while (index != -1) {
            camel.append(underscored, from, index);
            if (index + 1 < underscored.length()) {
                camel.append(Character.toUpperCase(underscored.charAt(index + 1)));
                from = index + 2;
            } else {
                from = index + 1;
                break;
            }
            index = underscored.indexOf('_', from);
        }

        camel.append(underscored.substring(from));

        return camel.toString();
    }

}
