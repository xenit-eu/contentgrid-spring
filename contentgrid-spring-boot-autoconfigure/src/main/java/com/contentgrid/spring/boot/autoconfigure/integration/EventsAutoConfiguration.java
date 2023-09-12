package com.contentgrid.spring.boot.autoconfigure.integration;

import com.contentgrid.spring.integration.events.ContentGridEventHandlerProperties;
import com.contentgrid.spring.integration.events.EntityChangeEventHandler;
import com.contentgrid.spring.integration.events.ChangeEventPublicationConfiguration;
import com.contentgrid.spring.integration.events.EntityChangeHibernateEventListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.integration.amqp.dsl.Amqp;

@AutoConfiguration
@ConditionalOnClass(EntityChangeHibernateEventListener.class)
@ConditionalOnBean(TypeConstrainedMappingJackson2HttpMessageConverter.class)
@AutoConfigureAfter(RepositoryRestMvcAutoConfiguration.class)
@Import(ChangeEventPublicationConfiguration.class)
public class EventsAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "contentgrid")
    ContentGridEventHandlerProperties contentGridEventHandlerProperties() {
        return new ContentGridEventHandlerProperties();
    }


    @ConditionalOnProperty(value = {"spring.rabbitmq.host"})
    @Configuration(proxyBeanMethods = false)
    static class EventsRabbitMqAutoConfiguration {

        @Bean
        EntityChangeEventHandler messageHandler(RabbitTemplate rabbitTemplate,
                ContentGridEventHandlerProperties config) {
            return () -> Amqp.outboundAdapter(rabbitTemplate)
                    .routingKey(config.getEvents().getRabbitmq().getRoutingKey())
                    .getObject();
        }
    }

}
