package com.contentgrid.spring.data.rest.webmvc.blueprint.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
public class EntityModel {

    private String name;
    private String title;
    private String domainType;
    private String description;

    @JsonInclude(Include.NON_EMPTY)
    @Builder.Default
    private List<AttributeModel> attributes = List.of();

    @JsonInclude(Include.NON_EMPTY)
    @Builder.Default
    private List<RelationModel> relations = List.of();

}
