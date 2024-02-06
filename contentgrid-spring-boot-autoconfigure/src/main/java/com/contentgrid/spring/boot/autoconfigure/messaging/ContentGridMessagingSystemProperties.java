package com.contentgrid.spring.boot.autoconfigure.messaging;

import java.util.UUID;
import lombok.Data;

@Data
public class ContentGridMessagingSystemProperties {
    private String deploymentId = new UUID(0, 0).toString();
    private String applicationId = new UUID(0, 0).toString();
}
