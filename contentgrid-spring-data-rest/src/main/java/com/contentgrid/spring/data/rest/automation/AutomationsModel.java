package com.contentgrid.spring.data.rest.automation;

import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.AttributeAnnotationSubjectModel;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.EntityAnnotationSubjectModel;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AnnotationSubjectModel.RelationAnnotationSubjectModel;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AutomationsModel {

    @NonNull List<AutomationModel> automations;

    @Value
    @Builder
    @Jacksonized
    public static class AutomationModel {

        @NonNull String id;
        @NonNull String system;
        @NonNull String name;
        @NonNull Map<String, Object> data;
        @NonNull List<AutomationAnnotationModel> annotations;
    }

    @Value
    @Builder
    @Jacksonized
    public static class AutomationAnnotationModel {

        @NonNull String id;
        @NonNull AnnotationSubjectModel subject;
        @NonNull Class<?> entityClass;
        @NonNull Map<String, Object> data;
    }

    @JsonTypeInfo(use = Id.NAME, property = "type")
    @JsonSubTypes({
            @Type(name = "entity", value = EntityAnnotationSubjectModel.class),
            @Type(name = "attribute", value = AttributeAnnotationSubjectModel.class),
            @Type(name = "relation", value = RelationAnnotationSubjectModel.class)
    })
    public sealed interface AnnotationSubjectModel {

        @Value
        @Builder
        @Jacksonized
        class EntityAnnotationSubjectModel implements AnnotationSubjectModel {

            @NonNull String entity;
        }

        @Value
        @Builder
        @Jacksonized
        class AttributeAnnotationSubjectModel implements AnnotationSubjectModel {

            @NonNull String entity;
            @NonNull String attribute;
        }

        @Value
        @Builder
        @Jacksonized
        class RelationAnnotationSubjectModel implements AnnotationSubjectModel {

            @NonNull String entity;
            @NonNull String relation;
        }
    }
}
