package com.contentgrid.spring.data.rest.links;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import com.contentgrid.spring.data.rest.webmvc.ProfileLinksResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.RepresentationModelProcessor;

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

    @Bean
    RepresentationModelProcessor<RepositoryLinksResource> contentGridRepositoryLinksResourceProcessor(Repositories repositories, ResourceMappings resourceMappings, EntityLinks entityLinks) {
        return new SpringDataRepositoryLinksResourceProcessor(repositories, resourceMappings, entityLinks);
    }

    @Bean
    RepresentationModelProcessor<ProfileLinksResource> contentGridProfileLinksResourceProcessor(Repositories repositories, ResourceMappings resourceMappings, RepositoryRestConfiguration configuration) {
        return new SpringDataProfileLinksResourceProcessor(repositories, resourceMappings, configuration);
    }

    @Bean
    RepresentationModelProcessor<CollectionModel<?>> contentGridSpringDataEmbeddedItemResourceProcessor() {
        return new SpringDataEmbeddedItemResourceProcessor();
    }
}
