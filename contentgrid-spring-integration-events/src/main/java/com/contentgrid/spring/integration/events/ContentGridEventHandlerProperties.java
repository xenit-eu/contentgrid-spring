package com.contentgrid.spring.integration.events;

import lombok.Data;

@Data
public class ContentGridEventHandlerProperties {
    private SystemProperties system = new SystemProperties();
    private EventProperties events = new EventProperties();

    @Data
    public static class SystemProperties {
        private String deploymentId;
        private String applicationId;
    }
    
    @Data
    public static class EventProperties {
        private String webhookConfigUrl;
        private RabbitMq rabbitmq = new RabbitMq();
    }
    
    @Data
    public static class RabbitMq {
        private String routingKey;
    }
}
