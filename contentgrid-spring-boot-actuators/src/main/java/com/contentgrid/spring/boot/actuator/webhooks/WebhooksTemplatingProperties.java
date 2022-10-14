package com.contentgrid.spring.boot.actuator.webhooks;

import java.util.Map;
import lombok.Data;

@Data
public class WebhooksTemplatingProperties {
    private Map<String, String> variables;
}
