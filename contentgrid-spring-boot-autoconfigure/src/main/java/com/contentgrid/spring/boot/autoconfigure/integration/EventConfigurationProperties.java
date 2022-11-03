package com.contentgrid.spring.boot.autoconfigure.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("contentgrid.events")
public class EventConfigurationProperties {

    static public final String DEFAULT_QUEUE_NAME = "contentgrid.events";

    private String queueName = DEFAULT_QUEUE_NAME;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
