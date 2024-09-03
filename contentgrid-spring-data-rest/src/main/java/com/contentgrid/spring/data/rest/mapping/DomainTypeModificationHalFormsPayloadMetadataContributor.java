package com.contentgrid.spring.data.rest.mapping;

import com.contentgrid.spring.data.rest.hal.forms.BasicPropertyMetadata;
import com.contentgrid.spring.data.rest.webmvc.HalFormsPayloadMetadataContributor;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.io.File;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.InputType;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class DomainTypeModificationHalFormsPayloadMetadataContributor implements HalFormsPayloadMetadataContributor {

    private final DomainTypeMapping formMapping;
    private final CollectionFiltersMapping searchMapping;
    // Optional means it only gets autowired if available
    private final Optional<MappingContext> contentMappingContext;

    private final boolean useMultipartHalForms;

    @Override
    public Stream<PropertyMetadata> contributeToCreateForm(Class<?> domainType) {
        return extractPropertyMetadataForForms(formMapping.forDomainType(domainType),
                domainType,
                "", // path prefix starts empty
                (prop) -> (!prop.isReadOnly() && !prop.isIgnored() && prop.findAnnotation(MimeType.class).isEmpty()
                        && prop.findAnnotation(
                        OriginalFileName.class).isEmpty()) || (useMultipartHalForms && prop.findAnnotation(
                        ContentId.class).isPresent()),
                this::propertyToMetadataForCreateForm
        );
    }

    @Override
    public Stream<PropertyMetadata> contributeToUpdateForm(Class<?> domainType) {
        return extractPropertyMetadataForForms(formMapping.forDomainType(domainType),
                domainType,
                "", // path prefix starts empty
                (prop) -> (!prop.isReadOnly() && !prop.isIgnored()),
                this::propertyToMetadataForUpdateForm
        )
                .filter(property -> !Objects.equals(property.getInputType(), HtmlInputType.URL_VALUE));
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
        if (useMultipartHalForms) {

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
        }

        // Default: use same property metadata as for update form
        return propertyToMetadataForUpdateForm(property, domainClass, path);
    }

    private PropertyMetadata propertyToMetadataForUpdateForm(Property property, Class<?> domainClass, String path) {
        var result = new BasicPropertyMetadata(path,
                property.getTypeInformation().toTypeDescriptor().getResolvableType())
                .withRequired(property.isRequired())
                .withReadOnly(false);

        var inputTypeAnnotation = property.findAnnotation(InputType.class);

        if (inputTypeAnnotation.isPresent()) {
            return result.withInputType(inputTypeAnnotation.get().value());
        }
        return result;
    }

    @Override
    public Stream<PropertyMetadata> contributeToSearchForm(Class<?> domainType) {
        return Stream.empty();
    }

    @FunctionalInterface
    interface PropertyMetadataFactory {

        PropertyMetadata build(Property property, Class<?> domainClass, String path);
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
