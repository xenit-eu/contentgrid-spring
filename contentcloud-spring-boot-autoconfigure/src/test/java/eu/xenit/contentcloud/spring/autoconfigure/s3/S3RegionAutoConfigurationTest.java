package eu.xenit.contentcloud.spring.autoconfigure.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

import java.util.Arrays;
import java.util.Properties;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class S3RegionAutoConfigurationTest {
    private Properties systemPropertiesBackup;

    @BeforeEach
    void backupSystemProperties() {
        systemPropertiesBackup = new Properties();
        systemPropertiesBackup.putAll(System.getProperties());
    }

    @AfterEach
    void restoreSystemProperties() {
        System.setProperties(systemPropertiesBackup);
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
    @ResourceLock(value = SYSTEM_PROPERTIES)
    void setsRegionWhenConfiguredExplicitly() {
        System.setProperty("spring.content.s3.region", "my-region");

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(TestConfig.class);
        applicationContext.refresh();

        S3Client client = applicationContext.getBean(S3Client.class);

        SdkClientConfiguration configuration = extractConfiguration(client);

        assertEquals(Region.of("my-region"), configuration.option(AwsClientOption.AWS_REGION));
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES)
    void setsFakeRegionWhenConfiguredWithEndpoint() {
        System.setProperty("spring.content.s3.endpoint", "https://some-endpoint:1234/");

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(TestConfig.class);
        applicationContext.refresh();

        S3Client client = applicationContext.getBean(S3Client.class);

        SdkClientConfiguration configuration = extractConfiguration(client);

        assertEquals(Region.of("fake"), configuration.option(AwsClientOption.AWS_REGION));
    }


    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES)
    void setsNoRegionWhenNotConfigured() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(TestConfig.class);

        BeanCreationException beanCreationException = assertThrows(BeanCreationException.class, () -> {
            applicationContext.refresh();
        });

        assertInstanceOf(SdkClientException.class, beanCreationException.getRootCause());
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES)
    void setsNoRegionWhenS3NotEnabled() {
        System.setProperty("spring.content.storage.type.default", "fs");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(TestConfig.class);
        applicationContext.refresh();

        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            applicationContext.getBean(S3Client.class);
        });
    }


    @Configuration
    @EnableAutoConfiguration
    @AutoConfigurationPackage
    public static class TestConfig {

    }

}