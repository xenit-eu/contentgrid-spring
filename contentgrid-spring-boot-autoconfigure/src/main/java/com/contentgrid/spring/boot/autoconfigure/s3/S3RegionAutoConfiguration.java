package com.contentgrid.spring.boot.autoconfigure.s3;

import com.contentgrid.spring.boot.autoconfigure.s3.S3RegionAutoConfiguration.S3AdditionalProperties;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration.S3Properties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;

@AutoConfiguration
// @AutoConfigureAfter uses a string because the class may not be present on the classpath,
// and we don't want an exception in that case
@AutoConfigureBefore(name="internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration")
@ConditionalOnClass({S3Client.class, S3ContentAutoConfiguration.class})
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "s3",
        matchIfMissing=true)
@EnableConfigurationProperties(S3AdditionalProperties.class)
public class S3RegionAutoConfiguration {

    @Data
    @ConfigurationProperties(prefix = "spring.content.s3")
    public static class S3AdditionalProperties {
        private String region;
    }

    /**
     * This component will be registered (and created) before S3Client,
     * and is thus able to set a property that is later picked up by the S3Client.
     */
    @Component
    @RequiredArgsConstructor
    static class S3RegionConfigurer implements InitializingBean, DisposableBean {
        private final S3Properties s3Properties;
        private final S3AdditionalProperties s3AdditionalProperties;

        private final static Object AWS_REGION_SENTINEL = new Object();
        private final static Object AWS_REGION_NON_EXISTENT = new Object();

        private Object previousAwsRegionProperty = AWS_REGION_SENTINEL;

        private void replaceRegion(String newRegion) {
            if(previousAwsRegionProperty == AWS_REGION_SENTINEL) {
                if(System.getProperties().contains("aws.region")) {
                    previousAwsRegionProperty = System.getProperty("aws.region");
                } else {
                    previousAwsRegionProperty = AWS_REGION_NON_EXISTENT;
                }

            }
            System.setProperty("aws.region", newRegion);
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            if(StringUtils.hasText(s3AdditionalProperties.getRegion())) {
                replaceRegion(s3AdditionalProperties.getRegion());
            } else if(StringUtils.hasText(s3Properties.endpoint)) {
                replaceRegion("none");
            }
        }

        @Override
        public void destroy() throws Exception {
            if (AWS_REGION_NON_EXISTENT.equals(previousAwsRegionProperty)) {
                System.getProperties().remove("aws.region");
            } else if(previousAwsRegionProperty != AWS_REGION_SENTINEL) {
                System.setProperty("aws.region", (String)previousAwsRegionProperty);
            }

        }
    }
}
