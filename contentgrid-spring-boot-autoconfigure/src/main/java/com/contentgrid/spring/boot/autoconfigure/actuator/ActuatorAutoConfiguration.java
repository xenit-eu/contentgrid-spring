package com.contentgrid.spring.boot.autoconfigure.actuator;

import com.contentgrid.spring.boot.actuator.SystemProperties;
import com.contentgrid.spring.boot.actuator.TemplateHelper;
import com.contentgrid.spring.boot.actuator.policy.PolicyActuator;
import com.contentgrid.spring.boot.actuator.webhooks.WebHooksConfigActuator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

@Configuration
@ConditionalOnClass({PolicyActuator.class, WebHooksConfigActuator.class})
public class ActuatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(PolicyActuator.class)
    PolicyActuator policyActuator(TemplateHelper templateHelper) {
        return new PolicyActuator("rego/policy.rego", templateHelper);
    }

    @Bean
    @ConditionalOnMissingBean(WebHooksConfigActuator.class)
    WebHooksConfigActuator webHooksConfigActuator(TemplateHelper templateHelper) {
        return new WebHooksConfigActuator("webhooks/config.json", templateHelper);
    }

    @Bean
    @ConfigurationProperties(prefix = "contentgrid")
    SystemProperties systemTemplatingProperties() {
        return new SystemProperties();
    }


    @Bean
    @ConditionalOnMissingBean(PropertyPlaceholderHelper.class)
    PropertyPlaceholderHelper getPropertyPlaceHolderHelper() {
        return new PropertyPlaceholderHelper(
                SystemPropertyUtils.PLACEHOLDER_PREFIX,
                SystemPropertyUtils.PLACEHOLDER_SUFFIX
        );
    }

    @Bean
    TemplateHelper templateHelper(PropertyPlaceholderHelper propertyPlaceholderHelper, SystemProperties system) {
        return new TemplateHelper(propertyPlaceholderHelper, system);
    }
}
