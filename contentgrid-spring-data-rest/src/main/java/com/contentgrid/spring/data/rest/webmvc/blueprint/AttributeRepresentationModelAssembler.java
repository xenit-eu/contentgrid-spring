package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedProperty;
import com.contentgrid.spring.data.rest.validation.AllowedValues;
import com.contentgrid.spring.data.rest.webmvc.blueprint.AttributeRepresentationModel.SearchParamRepresentationModel;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embedded;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.mediatype.MessageResolver;

@RequiredArgsConstructor
public class AttributeRepresentationModelAssembler {

    private final CollectionFiltersMapping collectionFiltersMapping;
    private final MessageResolver messageResolver;

    public Optional<AttributeRepresentationModel> toModel(RootResourceInformation information, List<Property> properties) {
        var property = properties.get(properties.size() - 1);
        var jsonProperty = new JacksonBasedProperty(property);
        if (jsonProperty.isIgnored()) {
            return Optional.empty();
        }

        // TODO: workaround that only works for com.contentgrid.spring.data.support.auditing.v1.UserMetadata properties
        if (jsonProperty.findAnnotation(JsonValue.class)
                .map(JsonValue::value)
                .orElse(false)) {
            return Optional.empty();
        }

        var attributes = new ArrayList<AttributeRepresentationModel>();
        var embedded = jsonProperty.findAnnotation(Embedded.class);
        if (embedded.isPresent()) {
            // TODO: incorrect if @JsonValue is present, use jsonProperty.nestedContainer() instead
            //  (but that one doesn't give us the java field names used for title, description and search params)
            property.nestedContainer()
                    .ifPresent(container -> container.doWithProperties(nestedProperty -> {
                        // TODO: json path and java path might differ when @JsonValue is present
                        var path = new ArrayList<>(properties);
                        path.add(nestedProperty);
                        this.toModel(information, path)
                                .ifPresent(attributes::add);
                    }));
        }

        var constraints = new ArrayList<AttributeConstraintRepresentationModel>();
        if (jsonProperty.isRequired()) {
            constraints.add(AttributeConstraintRepresentationModel.required());
        }
        if (jsonProperty.isUnique()) {
            constraints.add(AttributeConstraintRepresentationModel.unique());
        }
        jsonProperty.findAnnotation(AllowedValues.class).ifPresent(allowedValues -> constraints.add(
                AttributeConstraintRepresentationModel.allowedValues(List.of(allowedValues.value()))
        ));
        
        var searchParams = new ArrayList<SearchParamRepresentationModel>();
        var propertyNames = properties.stream().map(Property::getName).toArray(String[]::new);
        collectionFiltersMapping.forProperty(information.getDomainType(), propertyNames).documented().filters()
                .map(filter -> new SearchParamRepresentationModel(filter.getFilterName(), readPrompt(information, filter.getFilterName()),
                        filter.getFilterType()))
                .forEachOrdered(searchParams::add);

        var attribute = AttributeRepresentationModel.builder()
                .name(jsonProperty.getName())
                .title(readTitle(information, properties))
                .description(readDescription(information, properties))
                .type(getType(jsonProperty))
                .readOnly(jsonProperty.isReadOnly())
                .required(jsonProperty.isRequired())
                .attributes(attributes)
                .constraints(constraints)
                .searchParams(searchParams)
                .build();

        return Optional.of(attribute);
    }

    private String getType(Property property) {
        var type = DataType.from(property.getTypeInformation().getType());
        if (type == null && property.findAnnotation(Embedded.class).isPresent()) {
            type = DataType.OBJECT;
        }
        return type == null ? null : type.name().toLowerCase();
    }

    private String readDescription(RootResourceInformation information, List<Property> properties) {
        String description = null;
        if (properties.size() == 1) {
            description = messageResolver.resolve(DescriptionMessageSourceResolvable.forProperty(information,
                    properties.get(0)));
        } else if (properties.size() > 1) {
            var type = properties.get(properties.size() - 2).getTypeInformation();
            var property = properties.get(properties.size() - 1);
            description = messageResolver.resolve(DescriptionMessageSourceResolvable.forNestedProperty(type, property));
        }
        return description == null ? "" : description;
    }

    private String readTitle(RootResourceInformation information, List<Property> properties) {
        TypeInformation<?> type;
        if (properties.size() > 1) {
            type = properties.get(properties.size() - 2).getTypeInformation();
        } else {
            type = information.getPersistentEntity().getTypeInformation();
        }
        return messageResolver.resolve(TitleMessageSourceResolvable.forProperty(type, properties.get(
                properties.size() - 1)));
    }

    private String readPrompt(RootResourceInformation information, String path) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                return new String[]{information.getDomainType().getName() + "." + path + "._prompt"};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        return messageResolver.resolve(resolvable);
    }

}
