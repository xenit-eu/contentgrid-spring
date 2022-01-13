package eu.xenit.contentcloud.spring.autoconfigure.s3;

import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration.S3Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
// @AutoConfigureAfter uses a string because the class may not be present on the classpath,
// and we don't want an exception in that case
@AutoConfigureBefore(name="internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration")
@ConditionalOnClass({S3Client.class, S3ContentAutoConfiguration.class})
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "s3",
        matchIfMissing=true)
public class S3RegionAutoConfiguration {
    @Component
    @ConfigurationProperties(prefix = "spring.content.s3")
    @Data
    public static class S3AdditionalProperties {
        private String region;
    }

    /**
     * This component will be registered (and created) before S3Client,
     * and is thus able to set a property that is later picked up by the S3Client.
     */
    @Component
    @AllArgsConstructor(onConstructor_= @Autowired)
    static class S3RegionConfigurer implements InitializingBean {
        private final S3Properties s3Properties;
        private final S3AdditionalProperties s3AdditionalProperties;

        @Override
        public void afterPropertiesSet() throws Exception {
            if(StringUtils.hasText(s3Properties.endpoint)) {
                System.setProperty("aws.region", "fake");
            }
            if(StringUtils.hasText(s3AdditionalProperties.getRegion())) {
                System.setProperty("aws.region", s3AdditionalProperties.getRegion());
            }
        }
    }
}
