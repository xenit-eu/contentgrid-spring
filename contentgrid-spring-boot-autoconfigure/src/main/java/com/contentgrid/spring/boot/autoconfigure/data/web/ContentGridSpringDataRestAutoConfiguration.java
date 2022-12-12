package com.contentgrid.spring.boot.autoconfigure.data.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.ContentGridSpringDataRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

@Configuration
@ConditionalOnClass({ContentGridSpringDataRestConfiguration.class, RepositoryRestMvcConfiguration.class})
@Import(ContentGridSpringDataRestConfiguration.class)
public class ContentGridSpringDataRestAutoConfiguration {

}