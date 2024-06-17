package com.contentgrid.spring.data.rest.automation;

import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.AttributeAnnotationSubjectModel;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.EntityAnnotationSubjectModel;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.RelationAnnotationSubjectModel;
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
                .subject(getSubjectRepresentation(annotation.getSubject()))
                .data(annotation.getData())
                .build();
    }

    @NonNull
    private static Map<String, String> getSubjectRepresentation(
            @NonNull AutomationsModel.AnnotationSubjectModel model) {
        if (model instanceof EntityAnnotationSubjectModel subject) {
            return Map.of(
                    "type", "entity",
                    "entity", subject.getEntity()
            );
        } else if (model instanceof AttributeAnnotationSubjectModel subject) {
            return Map.of(
                    "type", "attribute",
                    "entity", subject.getEntity(),
                    "attribute", subject.getAttribute()
            );
        } else if (model instanceof RelationAnnotationSubjectModel subject) {
            return Map.of(
                    "type", "relation",
                    "entity", subject.getEntity(),
                    "relation", subject.getRelation()
            );
        }

        throw new IllegalArgumentException("Unknown annotation-subject type: %s".formatted(model));
    }
}