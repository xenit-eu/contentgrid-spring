package com.contentgrid.spring.boot.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class S3RegionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(S3RegionAutoConfiguration.class, S3ContentAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @AutoConfigurationPackage
    public static class TestConfig {

    }

    @SneakyThrows
    private static SdkClientConfiguration extractConfiguration(S3Client s3Client) {
        Class<? extends S3Client> s3clazz = s3Client.getClass();
        var sdkClientConfigField = Arrays.stream(s3clazz.getDeclaredFields())
                .filter(field -> field.getType() == SdkClientConfiguration.class)
                .findAny()
                .get();

        sdkClientConfigField.setAccessible(true);
        return (SdkClientConfiguration) sdkClientConfigField.get(s3Client);
    }

    @Test
    @ResourceLock(SYSTEM_PROPERTIES)
    void setsRegionWhenConfiguredExplicitly() {
        contextRunner
                .withPropertyValues("spring.content.s3.region=my-region")
                .run(context -> {
                    S3Client client = context.getBean(S3Client.class);

                    SdkClientConfiguration configuration = extractConfiguration(client);

                    assertEquals(Region.of("my-region"), configuration.option(AwsClientOption.AWS_REGION));
                });
    }

    @Test
    @ResourceLock(SYSTEM_PROPERTIES)
    void setsFakeRegionWhenConfiguredWithEndpoint() {
        contextRunner
                .withPropertyValues("spring.content.s3.endpoint=http://some-endpoint:1234/")
                .run(context -> {
                    S3Client client = context.getBean(S3Client.class);

                    SdkClientConfiguration configuration = extractConfiguration(client);

                    assertEquals(Region.of("none"), configuration.option(AwsClientOption.AWS_REGION));
                });
    }


    @Test
    @ResourceLock(SYSTEM_PROPERTIES)
    void setsNoRegionWhenNotConfigured() {
        contextRunner
                .run(context -> {
                    assertThat(context).getFailure().hasRootCauseInstanceOf(SdkClientException.class);
                });
    }

    @Test
    @ResourceLock(SYSTEM_PROPERTIES)
    void setsNoRegionWhenS3NotOnClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(S3Client.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(S3Client.class);
                });
    }

    @Test
    @ResourceLock(SYSTEM_PROPERTIES)
    void setsNoRegionWhenS3NotEnabled() {
        contextRunner
                .withPropertyValues("spring.content.storage.type.default=fs")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(S3Client.class);
                });
    }




}