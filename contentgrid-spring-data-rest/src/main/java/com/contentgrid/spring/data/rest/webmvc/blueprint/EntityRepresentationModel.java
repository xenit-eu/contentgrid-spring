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
import lombok.With;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.hateoas.server.core.Relation;

@Builder
@AllArgsConstructor
@Getter
@Relation(BlueprintLinkRelations.ENTITY_STRING)
public class EntityRepresentationModel extends RepresentationModel<EntityRepresentationModel> {

    @NonNull
    private final String name;
    @JsonInclude(Include.NON_EMPTY)
    private final String title;

    private final String description;

    @JsonIgnore
    @With
    @Builder.Default
    private final Collection<AttributeRepresentationModel> attributes = List.of();

    @JsonIgnore
    @With
    @Builder.Default
    private final Collection<RelationRepresentationModel> relations = List.of();

    @JsonUnwrapped
    @JsonProperty
    @JsonInclude(Include.NON_NULL)
    public CollectionModel<EmbeddedWrapper> getEmbeddeds() {
        var embeddedWrappers = new EmbeddedWrappers(true);

        return CollectionModel.of(List.of(
                embeddedWrappers.wrap(attributes, BlueprintLinkRelations.ATTRIBUTE),
                embeddedWrappers.wrap(relations, BlueprintLinkRelations.RELATION)
        ));
    }
}
