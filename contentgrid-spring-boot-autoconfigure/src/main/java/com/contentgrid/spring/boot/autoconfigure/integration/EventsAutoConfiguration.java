package com.contentgrid.spring.boot.autoconfigure.integration;

import javax.persistence.EntityManagerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

import com.contentgrid.spring.boot.actuator.ContentGridApplicationProperties;
import com.contentgrid.spring.integration.events.ContentGridEventHandlerProperties;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher;
import com.contentgrid.spring.integration.events.ContentGridHalAssembler;
import com.contentgrid.spring.integration.events.ContentGridMessageHandler;
import com.contentgrid.spring.integration.events.ContentGridPublisherEventListener;
import com.contentgrid.spring.integration.events.EntityToPersistentEntityResourceTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnClass(ContentGridPublisherEventListener.class)
@ConditionalOnBean(TypeConstrainedMappingJackson2HttpMessageConverter.class)
@AutoConfigureAfter(RepositoryRestMvcAutoConfiguration.class)
@IntegrationComponentScan(basePackageClasses = ContentGridEventPublisher.class)
@EnableConfigurationProperties( { EventConfigurationProperties.class })
public class EventsAutoConfiguration {

    @Bean
    IntegrationFlow contentGridEventsFlow(EventConfigurationProperties config,
            ObjectProvider<ContentGridMessageHandler> handlers,
            @Qualifier("halJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter typeConstrainedMappingJackson2HttpMessageConverter,
            ApplicationContext context) {

        ObjectMapper halObjectMapper = typeConstrainedMappingJackson2HttpMessageConverter
                .getObjectMapper();

        IntegrationFlowBuilder builder = IntegrationFlows
                .from(ContentGridEventPublisher.CONTENTGRID_EVENT_CHANNEL)
                .transform(new EntityToPersistentEntityResourceTransformer(
                        new ContentGridHalAssembler(context)))
                .transform(Transformers.toJson(new Jackson2JsonObjectMapper(halObjectMapper),
                        MediaTypes.HAL_JSON_VALUE));

        handlers.stream().map(ContentGridMessageHandler::get).forEach(builder::handle);
        return builder.get();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "contentgrid")
    ContentGridEventHandlerProperties contentGridEventHandlerProperties() {
        return new ContentGridEventHandlerProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    ContentGridPublisherEventListener contentGridPublisherEventListener(
            ContentGridEventPublisher contentGridEventPublisher,
            EntityManagerFactory entityManagerFactory, ContentGridEventHandlerProperties properties) {
        return new ContentGridPublisherEventListener(contentGridEventPublisher,
                entityManagerFactory, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = { "spring.rabbitmq.host" })
    static class EventsRabbitMqAutoConfiguration {

        @Bean
        ContentGridMessageHandler messageHandler(RabbitTemplate rabbitTemplate,
                EventConfigurationProperties config) {
            return () -> Amqp.outboundAdapter(rabbitTemplate)
                    .routingKey(config.getRabbitmq().getRoutingKey());
        }
    }

}
