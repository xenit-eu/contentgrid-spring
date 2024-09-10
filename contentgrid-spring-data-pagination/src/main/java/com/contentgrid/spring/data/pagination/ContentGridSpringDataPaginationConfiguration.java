package com.contentgrid.spring.data.pagination;

import com.contentgrid.spring.data.pagination.jpa.AggregateItemCountStrategy;
import com.contentgrid.spring.data.pagination.jpa.ExplainEstimateItemCountStrategy;
import com.contentgrid.spring.data.pagination.jpa.JpaQuerydslItemCountStrategy;
import com.contentgrid.spring.data.pagination.jpa.TimedDirectCountItemCountStrategy;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationConfiguration {

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
