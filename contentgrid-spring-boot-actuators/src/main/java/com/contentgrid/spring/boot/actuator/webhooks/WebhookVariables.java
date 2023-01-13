package com.contentgrid.spring.boot.actuator.webhooks;

import com.contentgrid.spring.boot.actuator.ContentGridApplicationProperties.SystemProperties;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

@Value
@Builder
public class WebhookVariables implements PlaceholderResolver {
    SystemProperties systemProperties;
    Map<String, String> userVariables;

    @Override
    public String resolvePlaceholder(String placeholderName) {
        if(placeholderName.startsWith("vars.")) {
            return userVariables.get(placeholderName.substring("vars.".length()));
        }
        switch(placeholderName) {
            case "system.application.id":
                return systemProperties.getApplicationId();
            case "system.deployment.id":
                return systemProperties.getDeploymentId();
            default:
                throw new IllegalArgumentException(String.format("Can not find a replacement for placeholder '%s'", placeholderName));
        }
    }
}
