package org.springframework.data.rest.webmvc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;

@Configuration(proxyBeanMethods = false)
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
                            applicationContext.getBean(RepositoryResourceMappings.class),
                            applicationContext.getBean(RepositoryEntityLinks.class),
                            applicationContext.getBean(SelfLinkProvider.class),
                            applicationContext.getBeanProvider(QuerydslBindingsFactory.class),
                            applicationContext.getBeanProvider(ContentGridRestProperties.class).getIfAvailable(ContentGridRestProperties::new));
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
