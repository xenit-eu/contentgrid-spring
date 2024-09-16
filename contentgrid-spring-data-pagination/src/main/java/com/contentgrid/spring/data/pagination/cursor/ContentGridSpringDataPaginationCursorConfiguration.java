package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationCursorConfiguration {

    @Bean
    CursorCodec simplePageBasedCursorCodec() {
        return new SimplePageBasedCursorCodec();
    }

    @Bean
    static BeanPostProcessor replaceHateoasPageableHandlerMethodArgumentResolverBeanPostProcessor(
            @Lazy HateoasSortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver,
            @Lazy CursorCodec cursorCodec
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HateoasPageableHandlerMethodArgumentResolver) {
                    return new HateoasPageableCursorHandlerMethodArgumentResolver(
                            sortHandlerMethodArgumentResolver,
                            cursorCodec
                    );
                }
                return bean;
            }
        };
    }

}
