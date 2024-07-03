package com.contentgrid.automations.rest;

import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = "cg:automation-annotation")
public class AutomationAnnotationRepresentationModel extends
        RepresentationModel<AutomationAnnotationRepresentationModel> {

    @NonNull
    private String id;
    @NonNull
    private Map<String, String> subject;
    @NonNull
    private Map<String, Object> data;

    public static AutomationAnnotationRepresentationModel from(
            @NonNull AutomationsModel.AutomationAnnotationModel annotation) {
        return AutomationAnnotationRepresentationModel.builder()
                .id(annotation.getId())
                .subject(annotation.getSubject())
                .data(annotation.getData())
                .build();
    }
}