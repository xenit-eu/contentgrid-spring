package com.contentgrid.spring.boot.autoconfigure.data.pagination;

import com.contentgrid.spring.data.pagination.cursor.ContentGridSpringDataPaginationCursorConfiguration;
import com.contentgrid.spring.data.pagination.web.ContentGridSpringDataPaginationWebConfiguration;
import com.contentgrid.spring.data.pagination.web.ItemCountPageMetadata;
import com.contentgrid.spring.data.pagination.web.ItemCountPageMetadataOmitLegacyPropertiesMixin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.ContentGridRestProperties;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;


@AutoConfiguration
@Import(
        {
                ContentGridSpringDataPaginationWebConfiguration.class,
                ContentGridSpringDataPaginationCursorConfiguration.class
        }
)
@ConditionalOnClass({
        ContentGridSpringDataPaginationWebConfiguration.class,
        PagedResourcesAssembler.class
})
@ConditionalOnWebApplication
public class WebPaginationAutoConfiguration {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer contentGridSpringDataPaginationOmitLegacyPropertiesObjectMapperBuilderCustomizer(
            ContentGridRestProperties restProperties
    ) {
        return jacksonObjectMapperBuilder -> {
            if (!restProperties.isExposeLegacyPageInfo()) {
                jacksonObjectMapperBuilder.mixIn(ItemCountPageMetadata.class,
                        ItemCountPageMetadataOmitLegacyPropertiesMixin.class);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RepositoryRestConfiguration.class})
    static class WebmvcPaginationConfiguration {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
            // This needs to run first, so it can be overridden
        PageableHandlerMethodArgumentResolverCustomizer contentGridPageableHandlerMethodArgumentResolverParametersCustomizer(
                RepositoryRestConfiguration repositoryRestConfiguration
        ) {
            return pageableResolver -> {
                pageableResolver.setPageParameterName(repositoryRestConfiguration.getPageParamName());
                pageableResolver.setSizeParameterName(repositoryRestConfiguration.getLimitParamName());
                pageableResolver.setFallbackPageable(
                        PageRequest.ofSize(repositoryRestConfiguration.getDefaultPageSize()));
                pageableResolver.setMaxPageSize(repositoryRestConfiguration.getMaxPageSize());
            };
        }
    }
}
