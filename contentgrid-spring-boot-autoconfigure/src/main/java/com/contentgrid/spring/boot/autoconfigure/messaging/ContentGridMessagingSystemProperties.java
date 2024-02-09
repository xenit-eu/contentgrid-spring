package com.contentgrid.spring.boot.autoconfigure.messaging;

import lombok.Data;

@Data
public class ContentGridMessagingSystemProperties {
    private String deploymentId;
    private String applicationId;
}
