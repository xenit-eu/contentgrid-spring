package com.contentgrid.spring.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentGridApplicationPropertiesConfiguration {
    @Bean
    @ConfigurationProperties(prefix = "contentgrid")
    ContentGridApplicationProperties contentgridApplicationProperties() {
        return new ContentGridApplicationProperties();
    }
}
