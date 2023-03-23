package com.contentgrid.spring.boot.actuator;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;

import lombok.Data;
import lombok.RequiredArgsConstructor;

public class ContentGridApplicationInfoContributor implements InfoContributor {

    private final ContentGridInfo contentGridInfo;

    public ContentGridApplicationInfoContributor(ContentGridInfo contentGridInfo) {
        this.contentGridInfo = contentGridInfo;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("ContentGrid", this.contentGridInfo);
    }
    
    @RequiredArgsConstructor
    @Data
    public static class ContentGridInfo {
        private final String applicationId;
        private final String deploymentId;
        private final String changesetId;
    }
}
