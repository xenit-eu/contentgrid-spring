package com.contentgrid.spring.data.rest.webmvc.blueprint.config;

import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.AllowedValuesConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.RequiredConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.UniqueConstraintModel;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "allowed-values", value = AllowedValuesConstraintModel.class),
        @JsonSubTypes.Type(name = "required", value = RequiredConstraintModel.class),
        @JsonSubTypes.Type(name = "unique", value = UniqueConstraintModel.class)
})
public sealed interface ConstraintModel {

    @Getter
    @NoArgsConstructor
    final class AllowedValuesConstraintModel implements ConstraintModel {

        private List<String> values = new ArrayList<>();
    }

    final class RequiredConstraintModel implements ConstraintModel {}

    final class UniqueConstraintModel implements ConstraintModel {}
}
