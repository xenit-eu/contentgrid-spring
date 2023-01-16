package com.contentgrid.spring.integration.events;

import lombok.Data;

@Data
public class ContentGridEventHandlerProperties {
    private SystemProperties system = new SystemProperties();

    @Data
    public static class SystemProperties {
        private String deploymentId;
        private String applicationId;
        private String webhookConfigUrl;
    }
}
