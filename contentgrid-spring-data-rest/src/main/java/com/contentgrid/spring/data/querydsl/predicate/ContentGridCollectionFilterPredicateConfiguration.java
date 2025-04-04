package com.contentgrid.spring.data.querydsl.predicate;

import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.repository.support.Repositories;

@Configuration(proxyBeanMethods = false)
public class ContentGridCollectionFilterPredicateConfiguration {

    @Bean
    SpringDataEntityId contentGridSpringDataEntityIdPredicate(Repositories repositories) {
        return new SpringDataEntityId(repositories);
    }

    @Bean
    SpringDataEntitySearch contentGridEntitySearchPredicate(
            CollectionFiltersMapping collectionFiltersMapping,
            ConversionService defaultConversionService,
            Repositories repositories,
            QuerydslBindingsFactory querydslBindingsFactory
    ) {
        return new SpringDataEntitySearch(collectionFiltersMapping, defaultConversionService, repositories, querydslBindingsFactory);
    }

}
