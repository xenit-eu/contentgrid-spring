package org.springframework.data.rest.webmvc;

import com.contentgrid.spring.data.querydsl.mapping.ContentGridCollectionFilterMappingConfiguration;
import com.contentgrid.spring.data.querydsl.predicate.ContentGridCollectionFilterPredicateConfiguration;
import com.contentgrid.spring.querydsl.resolver.CollectionFilterParamPredicateResolver;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.rest.webmvc.QuerydslBindingsPredicateResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;

@Configuration(proxyBeanMethods = false)
@Import({ContentGridCollectionFilterMappingConfiguration.class, ContentGridCollectionFilterPredicateConfiguration.class})
public class ContentGridSpringDataRestConfiguration {

    @Bean
    public BeanPostProcessor replaceRepositoryPropertyReferenceController(ApplicationContext applicationContext) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RepositoryPropertyReferenceController delegate) {
                    return new DelegatingRepositoryPropertyReferenceController(
                            delegate,
                            applicationContext.getBean(Repositories.class),
                            applicationContext.getBean(RepositoryEntityLinks.class),
                            applicationContext.getBean(SelfLinkProvider.class),
                            applicationContext.getBean(CollectionFiltersMapping.class),
                            applicationContext.getBeanProvider(ContentGridRestProperties.class).getIfAvailable(ContentGridRestProperties::new));
                }

                return bean;
            }
        };
    }
    
    @Bean
    public BeanPostProcessor replaceProfileController() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof ProfileController delegate) {
                    return new DelegatingProfileController(delegate);
                }
                return bean;
            }
        };
    }

    @Bean
    public BeanPostProcessor replaceQuerydslWithCollectionFilterParamPredicateResolver(ApplicationContext applicationContext) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof QuerydslBindingsPredicateResolver) {
                    return new CollectionFilterParamPredicateResolver(
                            applicationContext.getBean(CollectionFiltersMapping.class),
                            applicationContext.getBean("defaultConversionService", ConversionService.class)
                    );
                }
                return bean;
            }
        };
    }

    @Bean
    public ContentRestConfigurer contentRestConfigurer() {
        return config -> config.setShortcutLinks(false);
    }
}
