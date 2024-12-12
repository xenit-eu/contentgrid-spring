package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.webmvc.blueprint.config.AttributeModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.RequiredConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.SearchParamModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.hateoas.server.core.Relation;

@Builder
@Getter
@AllArgsConstructor
@Relation(BlueprintLinkRelations.ATTRIBUTE_STRING)
public class AttributeProfileRepresentationModel extends RepresentationModel<AttributeProfileRepresentationModel> {

    @NonNull
    private final String name;
    @JsonInclude(Include.NON_EMPTY)
    private final String title;

    @NonNull
    private final String type;

    private final String description;

    @JsonInclude(Include.NON_DEFAULT)
    private final boolean readOnly;

    @JsonInclude(Include.NON_DEFAULT)
    private final boolean required;

    @JsonIgnore
    private final Collection<AttributeConstraintRepresentationModel> constraints;
    @JsonIgnore
    private final Collection<SearchParamRepresentationModel> searchParams;
    @JsonIgnore
    private final Collection<AttributeProfileRepresentationModel> attributes;

    @JsonProperty
    @JsonUnwrapped
    public CollectionModel<EmbeddedWrapper> getEmbeddeds() {
        var embeddedWrappers = new EmbeddedWrappers(true);

        return CollectionModel.of(List.of(
                embeddedWrappers.wrap(constraints, BlueprintLinkRelations.CONSTRAINT),
                embeddedWrappers.wrap(searchParams, BlueprintLinkRelations.SEARCH_PARAM),
                embeddedWrappers.wrap(attributes, BlueprintLinkRelations.ATTRIBUTE)
        ));
    }

    public static AttributeProfileRepresentationModel from(AttributeModel attribute) {
        var isRequired = attribute.getConstraints().stream()
                .anyMatch(RequiredConstraintModel.class::isInstance);
        var constraints = attribute.getConstraints().stream()
                .map(AttributeConstraintRepresentationModel::from)
                .filter(Objects::nonNull)
                .toList();
        var searchParams = attribute.getSearchParams().stream()
                .map(SearchParamRepresentationModel::from)
                .toList();
        var attributes = attribute.getAttributes().stream()
                .map(AttributeProfileRepresentationModel::from)
                .toList();

        return AttributeProfileRepresentationModel.builder()
                .name(attribute.getName())
                .title(attribute.getTitle())
                .type(attribute.getType())
                .description(attribute.getDescription())
                .readOnly(attribute.isReadOnly())
                .required(isRequired)
                .constraints(constraints)
                .searchParams(searchParams)
                .attributes(attributes)
                .build();
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @Relation(BlueprintLinkRelations.SEARCH_PARAM_STRING)
    public static class SearchParamRepresentationModel {

        String name;
        @JsonInclude(Include.NON_EMPTY)
        String title;
        String type;

        public static SearchParamRepresentationModel from(SearchParamModel searchParam) {
            return SearchParamRepresentationModel.builder()
                    .name(searchParam.getName())
                    .title(searchParam.getTitle())
                    .type(searchParam.getType())
                    .build();
        }
    }
}
