package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.data.pagination.cursor.CursorCodec;
import java.util.function.Supplier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
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
            @Lazy HateoasPageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver,
            ObjectProvider<CursorCodec> cursorCodec
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof PagedResourcesAssembler<?>) {
                    return new ItemCountPageResourceAssembler<>(
                            pageableHandlerMethodArgumentResolver,
                            org.springframework.data.util.Lazy.of(cursorCodec::getIfUnique)
                    );
                }
                return bean;
            }
        };
    }

}
