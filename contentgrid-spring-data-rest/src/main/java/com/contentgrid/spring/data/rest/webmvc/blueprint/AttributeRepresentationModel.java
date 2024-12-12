package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Collection;
import java.util.List;
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
public class AttributeRepresentationModel extends RepresentationModel<AttributeRepresentationModel> {

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
    @Builder.Default
    private final Collection<AttributeConstraintRepresentationModel> constraints = List.of();

    @JsonIgnore
    @Builder.Default
    private final Collection<SearchParamRepresentationModel> searchParams = List.of();

    @JsonIgnore
    @Builder.Default
    private final Collection<AttributeRepresentationModel> attributes = List.of();

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

    @Builder
    @Getter
    @AllArgsConstructor
    @Relation(BlueprintLinkRelations.SEARCH_PARAM_STRING)
    public static class SearchParamRepresentationModel {

        String name;
        @JsonInclude(Include.NON_EMPTY)
        String title;
        String type;

    }
}
