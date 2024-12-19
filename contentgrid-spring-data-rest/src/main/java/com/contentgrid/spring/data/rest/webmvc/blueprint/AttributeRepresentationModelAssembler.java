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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.mediatype.InputTypeFactory;
import org.springframework.hateoas.mediatype.MessageResolver;

@RequiredArgsConstructor
public class AttributeRepresentationModelAssembler {

    private static final InputTypeFactory INPUT_TYPE_FACTORY;

    static {
        INPUT_TYPE_FACTORY = SpringFactoriesLoader.loadFactories(InputTypeFactory.class,
                AttributeRepresentationModelAssembler.class.getClassLoader()).get(0);
    }

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
        // TODO: How to distinguish between decimals and integers?
        var type = INPUT_TYPE_FACTORY.getInputType(property.getTypeInformation().getType());
        if (type == null && property.findAnnotation(Embedded.class).isPresent()) {
            type = "object";
        }
        return type;
    }

    private String readDescription(RootResourceInformation information, List<Property> properties) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                var message = properties.stream()
                        .map(Property::getName)
                        .collect(Collectors.joining("."));
                return new String[]{information.getResourceMetadata().getItemResourceDescription().getMessage() + "." + message};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        var description = messageResolver.resolve(resolvable);
        return description == null ? "" : description;
    }

    private String readTitle(RootResourceInformation information, List<Property> properties) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                var message = properties.stream()
                        .map(Property::getName)
                        .collect(Collectors.joining("."));
                return new String[]{information.getDomainType().getName() + "." + message + "._title"};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        return messageResolver.resolve(resolvable);
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
