package com.contentgrid.spring.data.querydsl.sort;

import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerAdapter;
import org.springframework.data.rest.webmvc.config.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.json.MappingAwareDefaultedPageableArgumentResolver;
import org.springframework.data.rest.webmvc.json.MappingAwarePageableArgumentResolver;
import org.springframework.data.rest.webmvc.json.MappingAwareSortArgumentResolver;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@Configuration(proxyBeanMethods = false)
public class ContentGridCollectionFilterSortConfiguration {

    /**
     * Replaces all "Mapping aware" resolvers with their plain bean variants
     */
    @Bean
    static BeanPostProcessor postProcessRepositoryRestMvcConfigurationRemovingMappingAwareResolvers(ApplicationContext applicationContext) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof RepositoryRestHandlerAdapter repositoryRestHandlerAdapter) {
                    repositoryRestHandlerAdapter.setArgumentResolvers(
                            replaceArgumentResolvers(
                                    repositoryRestHandlerAdapter.getArgumentResolvers()
                            )
                    );
                }
                return bean;
            }

            private List<HandlerMethodArgumentResolver> replaceArgumentResolvers(
                    @Nullable List<HandlerMethodArgumentResolver> argumentResolvers
            ) {
                if(argumentResolvers == null) {
                    argumentResolvers = new ArrayList<>();
                } else {
                    argumentResolvers = new ArrayList<>(argumentResolvers);
                }

                // Remove all mapping aware argument resolvers
                argumentResolvers.removeIf(resolver -> {
                    return resolver instanceof MappingAwareDefaultedPageableArgumentResolver || resolver instanceof MappingAwarePageableArgumentResolver || resolver instanceof MappingAwareSortArgumentResolver;
                });

                argumentResolvers.addAll(0, List.of(
                        new PassthroughDefaultedPageableMethodArgumentResolver(resolveBean(PageableHandlerMethodArgumentResolver.class)),
                        fromBean(PageableHandlerMethodArgumentResolver.class),
                        fromBean(SortHandlerMethodArgumentResolver.class)
                ));

                return argumentResolvers;
            }

            private <T> Lazy<T> resolveBean(Class<T> beanClass) {
                return Lazy.of(() -> applicationContext.getBean(beanClass));
            }

            private HandlerMethodArgumentResolver fromBean(Class<? extends HandlerMethodArgumentResolver> beanClass) {
                return new LazyResolvingHandlerMethodArgumentResolver<>(resolveBean(beanClass));
            }
        };
    }

    @Bean
    static BeanPostProcessor postProcessSortHandlerMethodArgumentResolver(
            @org.springframework.context.annotation.Lazy CollectionFiltersMapping collectionFiltersMapping,
            @org.springframework.context.annotation.Lazy ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof SortArgumentResolver sortArgumentResolver) {
                    return new CollectionFilterSortHandlerMethodArgumentResolver(sortArgumentResolver, collectionFiltersMapping, resourceMetadataHandlerMethodArgumentResolver);
                }
                return bean;
            }
        };
    }
}
