package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Builder
@Getter
@AllArgsConstructor
@Relation(BlueprintLinkRelations.RELATION_STRING)
public class RelationRepresentationModel extends RepresentationModel<RelationRepresentationModel> {

    @NonNull
    private final String name;
    @JsonInclude(Include.NON_EMPTY)
    private final String title;

    private final String description;

    @JsonProperty("many_source_per_target")
    private final boolean manySourcePerTarget;
    @JsonProperty("many_target_per_source")
    private final boolean manyTargetPerSource;

    private final boolean required;
}
