package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedProperty;
import com.contentgrid.spring.data.rest.validation.AllowedValues;
import com.contentgrid.spring.data.rest.webmvc.blueprint.AttributeRepresentationModel.SearchParamRepresentationModel;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import jakarta.persistence.Embedded;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.rest.webmvc.RootResourceInformation;
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

        String type = jsonProperty.getTypeInformation().getType().getSimpleName();

        var attributes = new ArrayList<AttributeRepresentationModel>();
        var embedded = jsonProperty.findAnnotation(Embedded.class);
        if (embedded.isPresent()) {
            // TODO: incorrect if @JsonValue is present, use jsonProperty.nestedContainer() instead
            //  (but this one doesn't give us the java field names used in title, description and search params)
            type = "object";
            property.nestedContainer()
                    .ifPresent(container -> container.doWithProperties(nestedProperty -> {
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
        var path = properties.stream().map(Property::getName).toArray(String[]::new);
        collectionFiltersMapping.forProperty(information.getDomainType(), path).documented().filters()
                .map(filter -> new SearchParamRepresentationModel(filter.getFilterName(), readPrompt(information, filter.getFilterName()),
                        filter.getFilterType()))
                .forEachOrdered(searchParams::add);

        var attribute = AttributeRepresentationModel.builder()
                .name(jsonProperty.getName())
                .title(readTitle(information, properties))
                .description(readDescription(information, properties))
                .type(type)
                .readOnly(jsonProperty.isReadOnly())
                .required(jsonProperty.isRequired())
                .attributes(attributes)
                .constraints(constraints)
                .searchParams(searchParams)
                .build();

        return Optional.of(attribute);
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
