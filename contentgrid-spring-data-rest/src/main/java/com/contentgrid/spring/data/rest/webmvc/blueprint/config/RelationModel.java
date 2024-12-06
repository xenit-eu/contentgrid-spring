package com.contentgrid.spring.data.rest.webmvc.blueprint.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class RelationModel {

    private String name;
    private String title;
    private String propertyName;

    @JsonProperty("target_entity")
    private String targetEntity;

    @JsonInclude(Include.NON_NULL)
    @Builder.Default
    private String description = "";

    @JsonProperty("many_source_per_target")
    private boolean manySourcePerTarget;
    @JsonProperty("many_target_per_source")
    private boolean manyTargetPerSource;

    private boolean required;
}
