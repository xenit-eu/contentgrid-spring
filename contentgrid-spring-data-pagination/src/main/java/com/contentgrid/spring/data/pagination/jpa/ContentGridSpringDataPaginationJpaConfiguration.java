package com.contentgrid.spring.data.pagination.jpa;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationJpaConfiguration {

    @Bean
    @Order(0)
    TimedDirectCountItemCountStrategy timedDirectCountItemCountStrategy() {
        return new TimedDirectCountItemCountStrategy();
    }

    @Bean
    @Order(20)
    ExplainEstimateItemCountStrategy explainEstimateItemCountStrategy() {
        return new ExplainEstimateItemCountStrategy();
    }

    @Primary
    @Bean
    JpaQuerydslItemCountStrategy jpaQuerydslItemCountStrategy(List<JpaQuerydslItemCountStrategy> strategies) {
        return new AggregateItemCountStrategy(strategies);
    }
}
