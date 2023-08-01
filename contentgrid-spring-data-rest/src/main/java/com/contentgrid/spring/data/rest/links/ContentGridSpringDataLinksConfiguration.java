package com.contentgrid.spring.data.rest.links;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataLinksConfiguration {
    @Bean
    RepositoryRestConfigurer contentGridLinkCollectorConfigurer(
            ObjectProvider<ContentGridLinkCollector> collectors
    ) {
        return new RepositoryRestConfigurer() {
            @Override
            public LinkCollector customizeLinkCollector(LinkCollector collector) {
                return new AggregateLinkCollector(collector, collectors);
            }
        };
    }

    @Bean
    ContentGridLinkCollector contentGridRelationLinkCollector(PersistentEntities entities, Associations associations, SelfLinkProvider selfLinkProvider) {
        return new SpringDataAssociationLinkCollector(entities, associations, selfLinkProvider);
    }

    @Bean
    CurieProviderCustomizer contentGridCurieProviderCustomizer() {
        return CurieProviderCustomizer.register(ContentGridLinkRelations.CURIE, ContentGridLinkRelations.TEMPLATE);
    }
}
