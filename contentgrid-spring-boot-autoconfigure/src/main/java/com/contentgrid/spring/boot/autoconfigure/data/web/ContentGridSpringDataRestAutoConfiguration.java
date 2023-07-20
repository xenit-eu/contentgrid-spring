package com.contentgrid.spring.boot.autoconfigure.data.web;

import com.contentgrid.spring.data.rest.webmvc.ContentGridSpringDataRestProfileConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.ContentGridRestProperties;
import org.springframework.data.rest.webmvc.ContentGridSpringDataRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

@Configuration
@ConditionalOnClass({ContentGridSpringDataRestConfiguration.class, RepositoryRestMvcConfiguration.class})
@Import({ContentGridSpringDataRestConfiguration.class, ContentGridSpringDataRestProfileConfiguration.class})
public class ContentGridSpringDataRestAutoConfiguration {
    @Bean
    @ConfigurationProperties("contentgrid.rest")
    ContentGridRestProperties contentGridRestProperties() {
        return new ContentGridRestProperties();
    }

}
