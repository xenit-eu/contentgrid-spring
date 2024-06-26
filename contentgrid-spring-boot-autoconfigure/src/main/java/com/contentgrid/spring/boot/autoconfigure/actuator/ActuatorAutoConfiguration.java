package com.contentgrid.spring.boot.autoconfigure.actuator;

import com.contentgrid.spring.common.ContentGridApplicationPropertiesConfiguration;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.contentgrid.spring.boot.actuator.ContentGridApplicationInfoContributor;
import com.contentgrid.spring.common.ContentGridApplicationProperties;
import com.contentgrid.spring.common.ContentGridApplicationProperties.SystemProperties;
import com.contentgrid.spring.boot.actuator.policy.PolicyActuator;
import com.contentgrid.spring.boot.actuator.policy.PolicyVariables;
import com.contentgrid.spring.boot.actuator.webhooks.WebHooksConfigActuator;
import com.contentgrid.spring.boot.actuator.webhooks.WebhookVariables;

import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@ConditionalOnClass({PolicyActuator.class, WebHooksConfigActuator.class, ConditionalOnAvailableEndpoint.class, ContentGridApplicationProperties.class})
@Import(ContentGridApplicationPropertiesConfiguration.class)
@Slf4j
public class ActuatorAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;


    @Configuration
    @ConditionalOnAvailableEndpoint(endpoint = PolicyActuator.class)
    class PolicyActuatorConfiguration {
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
        @ConditionalOnBean(PolicyActuator.class)
        ContentGridExposedActuatorEndpoint policyActuatorExposedEndpoint() {
            return new ContentGridExposedActuatorEndpoint(PolicyActuator.class);
        }
    }


    @Configuration
    @ConditionalOnAvailableEndpoint(endpoint = WebHooksConfigActuator.class)
    class WebhooksConfigActuatorConfiguration {
        @Bean
        WebHooksConfigActuator webHooksConfigActuator(WebhookVariables webhookVariables) {
            return new WebHooksConfigActuator(applicationContext.getResource("classpath:eventhandler/webhooks.json"),
                    webhookVariables);
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
        @ConditionalOnBean(WebHooksConfigActuator.class)
        ContentGridExposedActuatorEndpoint webhooksActuatorExposedEndpoint() {
            return new ContentGridExposedActuatorEndpoint(WebHooksConfigActuator.class);
        }
    }

    @Bean
    InfoContributor buildInfoContributor(ContentGridApplicationProperties applicationProperties) {
        String changeset = null;
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("META-INF/contentgrid.properties"));
            changeset = properties.getProperty("changeset");
        } catch (IOException e) {
            log.warn("Could not load META-INF/contentgrid.properties", e);
        }

        SystemProperties systemProperties = applicationProperties.getSystem();
        ContentGridApplicationInfoContributor contributor = new ContentGridApplicationInfoContributor(
                new ContentGridApplicationInfoContributor.ContentGridInfo(
                        systemProperties.getApplicationId(), systemProperties.getDeploymentId(), changeset));
        
        log.info("""          
          ContentGrid info:
                    
             ApplicationId: %s
              DeploymentId: %s
               ChangesetId: %s
          """.formatted(systemProperties.getApplicationId(), systemProperties.getDeploymentId(), changeset));
        
        return contributor;
    }
}
