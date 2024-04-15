package com.contentgrid.spring.boot.autoconfigure.s3;

import com.contentgrid.spring.boot.autoconfigure.s3.S3AutoConfiguration.S3AdditionalProperties;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration.S3Properties;
import java.net.URI;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@AutoConfiguration
// @AutoConfigureAfter uses a string because the class may not be present on the classpath,
// and we don't want an exception in that case
@AutoConfigureBefore(name="internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration")
@ConditionalOnClass({S3Client.class, S3Properties.class})
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "s3",
        matchIfMissing=true)
@EnableConfigurationProperties(S3AdditionalProperties.class)
public class S3AutoConfiguration {

    @Data
    @ConfigurationProperties(prefix = "spring.content.s3")
    public static class S3AdditionalProperties {
        private String region;
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Client amazonS3(S3Properties props, S3AdditionalProperties additionalProps) {
        S3ClientBuilder builder = S3Client.builder();

        if (StringUtils.hasText(props.endpoint)) {
            builder.endpointOverride(URI.create(props.endpoint));
            builder.region(Region.of("none"));
        }

        if (StringUtils.hasText(props.accessKey) && StringUtils.hasText(props.secretKey)) {
            AwsCredentialsProvider provider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.accessKey, props.secretKey));
            builder.credentialsProvider(provider);
        }

        if (props.pathStyleAccess) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (StringUtils.hasText(additionalProps.region)) {
            builder.region(Region.of(additionalProps.region));
        }

        return builder.build();
    }
}
