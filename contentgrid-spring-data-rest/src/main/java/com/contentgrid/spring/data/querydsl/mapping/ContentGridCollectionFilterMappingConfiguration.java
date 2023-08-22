package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.plugin.core.PluginRegistry;

@Configuration(proxyBeanMethods = false)
public class ContentGridCollectionFilterMappingConfiguration {

    @Bean
    CollectionFiltersMapping collectionFiltersMapping(
            Repositories repositories,
            ListableBeanFactory beanFactory,
            QuerydslBindingsFactory querydslBindingsFactory
    ) {
        return new CollectionFiltersMappingImpl(
                repositories,
                new BeanFactoryPredicateFactoryInstantiator(
                        beanFactory,
                        new DirectPredicateFactoryInstantiator()
                ),
                querydslBindingsFactory.getEntityPathResolver(),
                2
        );
    }

}
