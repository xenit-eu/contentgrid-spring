package com.contentgrid.spring.boot.autoconfigure.data.pagination;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@AutoConfiguration
@ConditionalOnClass({RepositoryRestConfiguration.class})
public class RestWebmvcPaginationAutoConfiguration {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    PageableHandlerMethodArgumentResolverCustomizer contentGridPageableHandlerMethodArgumentResolverParametersCustomizer(
            RepositoryRestConfiguration repositoryRestConfiguration
    ) {
        return pageableResolver -> {
            pageableResolver.setPageParameterName(repositoryRestConfiguration.getPageParamName());
            pageableResolver.setSizeParameterName(repositoryRestConfiguration.getLimitParamName());
            pageableResolver.setFallbackPageable(PageRequest.ofSize(repositoryRestConfiguration.getDefaultPageSize()));
            pageableResolver.setMaxPageSize(repositoryRestConfiguration.getMaxPageSize());
        };

    }
}
