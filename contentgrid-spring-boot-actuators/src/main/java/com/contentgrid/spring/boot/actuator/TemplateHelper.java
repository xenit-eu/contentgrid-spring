package com.contentgrid.spring.boot.actuator;

import java.util.HashMap;
import java.util.Map;
import org.springframework.util.PropertyPlaceholderHelper;

public class TemplateHelper {
    private static final String SYSTEM_DEPLOYMENT_ID = "system.deployment.id";
    private static final String SYSTEM_DEPLOYMENT_ID_SAFE = "system.deployment.id.safe";
    private static final String SYSTEM_APPLICATION_ID = "system.application.id";
    private static final String SYSTEM_APPLICATION_ID_SAFE = "system.application.id.safe";
    private static final String VARIABLES_PREFIX = "variables.";

    private final PropertyPlaceholderHelper propertyPlaceholderHelper;

    private final Map<String, String> systemVariables = new HashMap<>();
    private final Map<String, String> systemAndUserVariables = new HashMap<>();

    public TemplateHelper(PropertyPlaceholderHelper propertyPlaceholderHelper, SystemProperties systemProperties) {
        this.propertyPlaceholderHelper = propertyPlaceholderHelper;

        this.systemVariables.put(SYSTEM_DEPLOYMENT_ID, systemProperties.getDeploymentId());
        this.systemVariables.put(SYSTEM_DEPLOYMENT_ID_SAFE, makeSafeId("deployment", systemProperties.getDeploymentId()));
        this.systemVariables.put(SYSTEM_APPLICATION_ID, systemProperties.getApplicationId());
        this.systemVariables.put(SYSTEM_APPLICATION_ID_SAFE, makeSafeId("application", systemProperties.getApplicationId()));

        this.systemAndUserVariables.putAll(systemVariables);
        systemProperties.getVariables().forEach((key, value) ->
                this.systemAndUserVariables.put(VARIABLES_PREFIX + key, value));
    }

    private String makeSafeId(String prefix, String id) {
        return prefix + id.replaceAll("[^A-Za-z\\d]", "");
    }

    private String replace(String template, Map<String, String> properties) {
        return this.propertyPlaceholderHelper.replacePlaceholders(template, properties::get);
    }

    public String templateSystemVars(String template) {
        return this.replace(template, systemVariables);
    }

    public String templateSystemAndUserVars(String template) {
        return this.replace(template, systemAndUserVariables);
    }
}
