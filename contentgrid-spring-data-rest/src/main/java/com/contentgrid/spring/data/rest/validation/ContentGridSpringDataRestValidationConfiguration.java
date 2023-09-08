package com.contentgrid.spring.data.rest.validation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.Validator;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestValidationConfiguration {
    @Bean
    BeanValidationRepositoryRestConfigurer beanValidationRepositoryRestConfigurer(@Lazy Validator validator) {
        return new BeanValidationRepositoryRestConfigurer(validator);
    }

}
