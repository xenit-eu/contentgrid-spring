package com.contentgrid.spring.data.querydsl.predicate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;

@Configuration(proxyBeanMethods = false)
public class ContentGridCollectionFilterPredicateConfiguration {

    @Bean
    SpringDataEntityId contentGridSpringDataEntityIdPredicate(Repositories repositories) {
        return new SpringDataEntityId(repositories);
    }

}
