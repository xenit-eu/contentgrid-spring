package com.contentgrid.spring.boot.actuator.policy;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

@Value
@Builder
public class PolicyVariables implements PlaceholderResolver {
    String policyPackageName;

    @Override
    public String resolvePlaceholder(String placeholderName) {
        switch(placeholderName) {
            case "system.policy.package":
                return policyPackageName;
            default:
                throw new IllegalArgumentException(String.format("Can not find a replacement for placeholder '%s'", placeholderName));
        }
    }
}
