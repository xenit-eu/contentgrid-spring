package com.contentgrid.spring.boot.actuator;

import java.util.Map;
import lombok.Data;

@Data
public class ContentGridApplicationProperties {
    String deploymentId;
    String applicationId;
    private Map<String, String> variables;
}
