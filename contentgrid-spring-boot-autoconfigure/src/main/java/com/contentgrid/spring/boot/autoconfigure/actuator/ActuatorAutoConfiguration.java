package com.contentgrid.spring.boot.autoconfigure.actuator;

import com.contentgrid.spring.boot.actuator.policy.PolicyActuator;
import com.contentgrid.spring.boot.actuator.policy.PolicyTemplatingProperties;
import com.contentgrid.spring.boot.actuator.webhooks.WebHooksConfigActuator;
import com.contentgrid.spring.boot.actuator.webhooks.WebhooksTemplatingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

@Configuration
public class ActuatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(PolicyActuator.class)
    PolicyActuator policyActuator(PolicyTemplatingProperties properties, PropertyPlaceholderHelper helper) {
        return new PolicyActuator("rego/policy.rego", properties, helper);
    }

    @Bean
    @ConditionalOnMissingBean(WebHooksConfigActuator.class)
    WebHooksConfigActuator webHooksConfigActuator(WebhooksTemplatingProperties properties, PropertyPlaceholderHelper helper) {
        return new WebHooksConfigActuator("webhooks/config.json",  properties, helper);
    }

    @Bean
    @ConfigurationProperties(prefix = "contentgrid.webhook")
    WebhooksTemplatingProperties webhooksTemplatingProperties() {
        return new WebhooksTemplatingProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "contentgrid.policy")
    PolicyTemplatingProperties policyTemplatingProperties() {
        return new PolicyTemplatingProperties();
    }

    @Bean
    @ConditionalOnMissingBean(PropertyPlaceholderHelper.class)
    PropertyPlaceholderHelper getPropertyPlaceHolderHelper() {
        return new PropertyPlaceholderHelper(
                SystemPropertyUtils.PLACEHOLDER_PREFIX,
                SystemPropertyUtils.PLACEHOLDER_SUFFIX
        );
    }
}
