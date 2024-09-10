package com.contentgrid.spring.data.pagination;

import com.contentgrid.spring.data.pagination.jpa.ExplainEstimateItemCountStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataPaginationConfiguration {

    @Bean
    ExplainEstimateItemCountStrategy estimateCountingStrategy() {
        return new ExplainEstimateItemCountStrategy();
    }

}
