package com.contentgrid.spring.data.rest.webmvc.blueprint.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class AttributeModel {

    private String name;
    private String title;
    private String propertyName;

    private String type;

    @JsonInclude(Include.NON_NULL)
    @Builder.Default
    private String description = "";

    private boolean readOnly;

    @JsonProperty("search_params")
    @JsonInclude(Include.NON_EMPTY)
    @Builder.Default
    private List<SearchParamModel> searchParams = List.of();

    @JsonInclude(Include.NON_EMPTY)
    @Builder.Default
    private List<ConstraintModel> constraints = List.of();

    @JsonInclude(Include.NON_EMPTY)
    @Builder.Default
    private List<AttributeModel> attributes = List.of();
}
