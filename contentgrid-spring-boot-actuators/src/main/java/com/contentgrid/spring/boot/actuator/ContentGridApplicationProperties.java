package com.contentgrid.spring.boot.actuator;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;

@Data
public class ContentGridApplicationProperties {
    private SystemProperties system = new SystemProperties();
    private Map<String, String> variables;

    @Data
    public static class SystemProperties {
        private String deploymentId;
        private String applicationId;
        private String policyPackage;
    }
}
