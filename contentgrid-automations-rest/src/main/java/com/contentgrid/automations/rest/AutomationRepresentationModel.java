package com.contentgrid.automations.rest;

import com.contentgrid.automations.rest.AutomationsModel.AutomationModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.lang.Nullable;

@Data
@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = IanaLinkRelations.ITEM_VALUE)
public class AutomationRepresentationModel extends RepresentationModel<AutomationRepresentationModel> {

    @NonNull
    private final String id;

    @NonNull
    private final String system;

    @NonNull
    private final String name;

    @Nullable
    @JsonInclude(Include.NON_NULL)
    private final Map<String, Object> data;

    @JsonInclude(Include.NON_NULL)
    @JsonUnwrapped
    @Nullable
    private final CollectionModel<AutomationAnnotationRepresentationModel> annotations;

    public static AutomationRepresentationModel from(AutomationModel automation) {
        return AutomationRepresentationModel.builder()
                .id(automation.getId())
                .system(automation.getSystem())
                .name(automation.getName())
                .build();
    }

    public static AutomationRepresentationModel expandedFrom(AutomationModel automation, CollectionModel<AutomationAnnotationRepresentationModel> annotations) {
        return AutomationRepresentationModel.builder()
                .id(automation.getId())
                .system(automation.getSystem())
                .name(automation.getName())
                .data(automation.getData())
                .annotations(annotations)
                .build();
    }
}
