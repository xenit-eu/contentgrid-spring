package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.webmvc.blueprint.AttributeConstraintRepresentationModel.AllowedValuesConstraintRepresentationModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.AttributeConstraintRepresentationModel.RequiredConstraintRepresentationModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.AttributeConstraintRepresentationModel.UniqueConstraintRepresentationModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.AllowedValuesConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.RequiredConstraintModel;
import com.contentgrid.spring.data.rest.webmvc.blueprint.config.ConstraintModel.UniqueConstraintModel;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.hateoas.server.core.Relation;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "allowed-values", value = AllowedValuesConstraintRepresentationModel.class),
        @JsonSubTypes.Type(name = "required", value = RequiredConstraintRepresentationModel.class),
        @JsonSubTypes.Type(name = "unique", value = UniqueConstraintRepresentationModel.class)
})
public sealed interface AttributeConstraintRepresentationModel {

    static AttributeConstraintRepresentationModel from(ConstraintModel constraint) {
        if (constraint instanceof AllowedValuesConstraintModel allowedValuesConstraint) {
            return allowedValues(allowedValuesConstraint.getValues());
        } else if (constraint instanceof RequiredConstraintModel) {
            return required();
        } else if (constraint instanceof UniqueConstraintModel) {
            return unique();
        } else {
            // We want to be able to update this without breaking applications
            return null;
        }
    }

    static AllowedValuesConstraintRepresentationModel allowedValues(List<String> values) {
        return AllowedValuesConstraintRepresentationModel.builder()
                .values(values)
                .build();
    }

    static RequiredConstraintRepresentationModel required() {
        return new RequiredConstraintRepresentationModel();
    }

    static UniqueConstraintRepresentationModel unique() {
        return new UniqueConstraintRepresentationModel();
    }

    @Builder
    @Value
    @Relation(BlueprintLinkRelations.CONSTRAINT_STRING)
    class AllowedValuesConstraintRepresentationModel implements AttributeConstraintRepresentationModel {

        @Builder.Default
        List<String> values = new ArrayList<>();
    }

    @Relation(BlueprintLinkRelations.CONSTRAINT_STRING)
    final class RequiredConstraintRepresentationModel implements AttributeConstraintRepresentationModel {}

    @Relation(BlueprintLinkRelations.CONSTRAINT_STRING)
    final class UniqueConstraintRepresentationModel implements AttributeConstraintRepresentationModel {}
}
