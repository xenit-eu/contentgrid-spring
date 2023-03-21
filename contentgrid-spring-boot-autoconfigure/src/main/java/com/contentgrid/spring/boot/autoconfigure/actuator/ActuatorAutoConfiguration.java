package com.contentgrid.spring.boot.autoconfigure.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.contentgrid.spring.boot.actuator.ContentGridApplicationInfoContributor;
import com.contentgrid.spring.boot.actuator.ContentGridApplicationProperties;
import com.contentgrid.spring.boot.actuator.policy.PolicyActuator;
import com.contentgrid.spring.boot.actuator.policy.PolicyVariables;
import com.contentgrid.spring.boot.actuator.webhooks.WebHooksConfigActuator;
import com.contentgrid.spring.boot.actuator.webhooks.WebhookVariables;

@Configuration
@ConditionalOnClass({PolicyActuator.class, WebHooksConfigActuator.class})
public class ActuatorAutoConfiguration {
    @Autowired
    private ApplicationContext applicationContext;
    
    @Bean
    @ConditionalOnProperty(name = "contentgrid.system.policyPackage")
    @ConditionalOnMissingBean(PolicyVariables.class)
    PolicyVariables policyVariables(ContentGridApplicationProperties applicationProperties) {
        return PolicyVariables.builder()
                .policyPackageName(applicationProperties.getSystem().getPolicyPackage())
                .build();
    }

    @Bean    
    @ConditionalOnBean(PolicyVariables.class)  
    PolicyActuator policyActuator(PolicyVariables policyVariables) {
        return new PolicyActuator(applicationContext.getResource("classpath:rego/policy.rego"), policyVariables);
    }

    @Bean
    WebHooksConfigActuator webHooksConfigActuator(WebhookVariables webhookVariables) {
        return new WebHooksConfigActuator(applicationContext.getResource("classpath:eventhandler/webhooks.json"), webhookVariables);
    }

    @Bean
    @ConditionalOnMissingBean(WebhookVariables.class)
    WebhookVariables webhookVariables(ContentGridApplicationProperties applicationProperties) {
        return WebhookVariables.builder()
                .systemProperties(applicationProperties.getSystem())
                .userVariables(applicationProperties.getVariables())
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "contentgrid")
    ContentGridApplicationProperties contentgridApplicationProperties() {
        return new ContentGridApplicationProperties();
    }

    @Bean
    InfoContributor buildInfoContributor(ContentGridApplicationProperties applicationProperties) {
        return new ContentGridApplicationInfoContributor(applicationProperties.getSystem());
    }
}
