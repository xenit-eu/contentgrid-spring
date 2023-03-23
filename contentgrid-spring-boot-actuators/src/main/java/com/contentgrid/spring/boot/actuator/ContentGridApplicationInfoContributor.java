package com.contentgrid.spring.boot.actuator;

import java.util.Map;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;

import com.contentgrid.spring.boot.actuator.ContentGridApplicationProperties.SystemProperties;

public class ContentGridApplicationInfoContributor implements InfoContributor {

    private final SystemProperties systemProperties;

    public ContentGridApplicationInfoContributor(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("ContentGrid", Map.of(
                "applicationId", systemProperties.getApplicationId(), 
                "deploymentId", systemProperties.getDeploymentId()));
    }
}
