package com.contentgrid.spring.integration.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;

@Configuration(proxyBeanMethods = false)
@IntegrationComponentScan(basePackageClasses = EntityChangeEventPublisher.class)
@EnableIntegration()
public class ChangeEventPublicationConfiguration {

    @Bean
    EntityChangeEventTransformer contentGridEntityChangeEventTransformer(
            @Qualifier("halJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter typeConstrainedMappingJackson2HttpMessageConverter,
            ApplicationContext applicationContext
    ) {
        return new EntityChangeEventTransformer(new EntityModelAssembler(applicationContext),
                typeConstrainedMappingJackson2HttpMessageConverter.getObjectMapper());
    }

    @Bean
    EntityChangeHibernateEventListener contentGridEntityChangeEventListener(
            EntityChangeEventPublisher entityChangeEventPublisher,
            EntityManagerFactory entityManagerFactory,
            Repositories repositories
    ) {
        return new EntityChangeHibernateEventListener(
                entityChangeEventPublisher,
                entityManagerFactory,
                repositories
        );
    }

    @Bean
    IntegrationFlow contentGridChangeEventsFlow(
            ObjectProvider<EntityChangeEventHandler> handlers,
            @Qualifier("halJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter typeConstrainedMappingJackson2HttpMessageConverter,
            EntityChangeEventTransformer entityChangeEventTransformer,
            ContentGridEventHandlerProperties eventHandlerProperties
    ) {
        ObjectMapper halObjectMapper = typeConstrainedMappingJackson2HttpMessageConverter.getObjectMapper();

        return IntegrationFlow
                .from(EntityChangeEventPublisher.CHANGE_EVENT_CHANNEL)
                .to(new PublishContentGridMessageFlow(
                                eventHandlerProperties,
                                entityChangeEventTransformer,
                                halObjectMapper,
                                handlers.stream().toList()
                        )
                );
    }

}
