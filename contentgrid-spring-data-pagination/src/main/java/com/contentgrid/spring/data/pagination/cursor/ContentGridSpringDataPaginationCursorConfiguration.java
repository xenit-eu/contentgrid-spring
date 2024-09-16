package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationCursorConfiguration {

    @Bean
    CursorCodec simplePageBasedCursorCodec() {
        return new SimplePageBasedCursorCodec();
    }

    @Bean
    static BeanPostProcessor replaceHateoasPageableHandlerMethodArgumentResolverBeanPostProcessor(
            @Lazy HateoasSortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver,
            @Lazy CursorCodec cursorCodec,
            ObjectProvider<PageableHandlerMethodArgumentResolverCustomizer> resolverCustomizers
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HateoasPageableHandlerMethodArgumentResolver) {
                    var resolver = new HateoasPageableCursorHandlerMethodArgumentResolver(
                            sortHandlerMethodArgumentResolver,
                            cursorCodec
                    );
                    resolverCustomizers.forEach(c -> c.customize(resolver));
                    return resolver;
                }
                return bean;
            }
        };
    }

}
