package com.contentgrid.automations.rest;

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
        @NonNull Map<String, String> subject;
        @NonNull Class<?> entityClass;
        @NonNull Map<String, Object> data;
    }
}
