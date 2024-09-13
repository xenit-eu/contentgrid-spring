package com.contentgrid.spring.data.pagination.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationWebConfiguration {

    @Bean
    static BeanPostProcessor replacePagedResourceAssemblerBeanPostProcessor(
            @Lazy HateoasPageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof PagedResourcesAssembler<?>) {
                    return new ItemCountPageResourceAssembler<>(pageableHandlerMethodArgumentResolver);
                }
                return bean;
            }
        };
    }

}
