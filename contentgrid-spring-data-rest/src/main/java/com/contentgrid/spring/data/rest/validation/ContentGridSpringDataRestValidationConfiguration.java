package com.contentgrid.spring.data.rest.validation;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.validation.Validator;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestValidationConfiguration {
    @Bean
    BeanValidationRepositoryEventListener beanValidationRepositoryEventListener(ObjectProvider<Validator> validator, PersistentEntities persistentEntities) {
        return new BeanValidationRepositoryEventListener(persistentEntities, validator);
    }

}
