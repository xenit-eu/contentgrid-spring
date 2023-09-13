package com.contentgrid.spring.data.rest.problem;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableHypermediaSupport(type = HypermediaType.HTTP_PROBLEM_DETAILS)
public class ContentGridProblemDetailsConfiguration {

    @Bean
    @Order(0)
    SpringDataRestRepositoryExceptionHandler contentGridSpringDataRestRepositoryExceptionHandler() {
        return new SpringDataRestRepositoryExceptionHandler();
    }

    @Bean
    @Order(0)
    SpringContentRestExceptionHandler contentGridSpringContentRestRepositoryExceptionHandler() {
        return new SpringContentRestExceptionHandler();
    }


}
