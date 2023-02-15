package com.contentgrid.spring.integration.events;

import java.util.UUID;
import org.springframework.util.Assert;
import lombok.Data;

@Data
public class ContentGridEventHandlerProperties {

    private SystemProperties system = new SystemProperties();
    private EventProperties events = new EventProperties();

    @Data
    public static class SystemProperties {
        private String deploymentId = new UUID(0, 0).toString();
        private String applicationId = new UUID(0, 0).toString();
    }

    @Data
    public static class EventProperties {

        private String webhookConfigUrl = "";
        private RabbitMq rabbitmq = new RabbitMq();
    }

    @Data
    public static class RabbitMq {
        private String routingKey = "contentgrid.events";
        
        public void setRoutingKey(String routingKey) {
            Assert.hasText(routingKey, "routing-key cannot be empty");
            this.routingKey = routingKey;
        }
    }
}
