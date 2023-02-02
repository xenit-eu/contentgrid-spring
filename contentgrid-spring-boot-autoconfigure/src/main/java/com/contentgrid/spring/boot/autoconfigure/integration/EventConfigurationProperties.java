package com.contentgrid.spring.boot.autoconfigure.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties("contentgrid.events")
public class EventConfigurationProperties {

    private RabbitMqConfigurationProperties rabbitmq = new RabbitMqConfigurationProperties();
    
    public RabbitMqConfigurationProperties getRabbitmq() {
        return rabbitmq;
    }
    
    public void setRabbitmq(RabbitMqConfigurationProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public static class RabbitMqConfigurationProperties {
        private String routingKey = "contentgrid.events";

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            Assert.hasText(routingKey, "routing-key cannot be empty");
            this.routingKey = routingKey;
        }
    }
}
