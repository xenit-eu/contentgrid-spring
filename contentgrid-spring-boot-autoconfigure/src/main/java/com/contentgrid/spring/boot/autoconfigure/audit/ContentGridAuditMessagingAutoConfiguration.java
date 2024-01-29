package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.handler.messaging.AuditEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.AuditEventToCloudEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.MessageSendingAuditHandler;
import com.contentgrid.spring.audit.handler.messaging.Jackson2AuditMessagingModule;
import com.contentgrid.spring.boot.autoconfigure.ContentGrid;
import com.contentgrid.spring.boot.autoconfigure.audit.ContentGridAuditMessagingAutoConfiguration.ContentGridAuditMessagingProperties;
import com.contentgrid.spring.boot.autoconfigure.messaging.ContentGridMessagingAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;
import java.net.URI;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.MessageSendingOperations;

@AutoConfiguration(
        after = ContentGridMessagingAutoConfiguration.class,
        before = ContentGridAuditLoggingAutoConfiguration.class
)
@ConditionalOnClass({MessageSendingOperations.class, MessageSendingAuditHandler.class})
@ConditionalOnBean(value = MessageSendingOperations.class, annotation = ContentGrid.class)
@EnableConfigurationProperties(ContentGridAuditMessagingProperties.class)
@ConditionalOnProperty(value = "contentgrid.audit.messaging.enabled", matchIfMissing = true)
public class ContentGridAuditMessagingAutoConfiguration {

    @Bean
    MessageSendingAuditHandler messageSendingAuditHandler(
            @ContentGrid MessageSendingOperations<String> sendingOperations,
            ContentGridAuditMessagingProperties auditProperties
    ) {
        return new MessageSendingAuditHandler(sendingOperations, auditProperties.getDestination());
    }

    @Bean
    @ContentGrid
    @ConditionalOnProperty("contentgrid.audit.messaging.source")
    @ConditionalOnClass({CloudEvent.class, AuditEventToCloudEventMessageConverter.class,
            CloudEventMessageConverter.class})
    @Order(10)
    MessageConverter auditEventToCloudEventMessageConverter(
            @ContentGrid(ContentGridAuditMessagingAutoConfiguration.class) ObjectMapper objectMapper,
            ContentGridAuditMessagingProperties auditProperties
    ) {
        return new AuditEventToCloudEventMessageConverter(
                new CloudEventMessageConverter(),
                objectMapper::writeValueAsBytes,
                auditProperties.getSource()
        );
    }

    @Bean
    @ContentGrid
    MessageConverter auditEventMessageConverter(
            @ContentGrid(ContentGridAuditMessagingAutoConfiguration.class) ObjectMapper objectMapper
    ) {
        return new AuditEventMessageConverter(objectMapper);
    }

    @Bean
    @ContentGrid(ContentGridAuditMessagingAutoConfiguration.class)
    ObjectMapper auditObjectMapper(ObjectProvider<ObjectMapper> objectMapper) {
        var originalMapper = objectMapper.getIfAvailable();
        ObjectMapper mapper;
        if (originalMapper != null) {
            mapper = originalMapper.copy();
        } else {
            mapper = new ObjectMapper();
        }

        mapper.registerModule(new Jackson2AuditMessagingModule());

        return mapper;

    }

    @ConfigurationProperties("contentgrid.audit.messaging")
    @Data
    static class ContentGridAuditMessagingProperties {

        private URI source;

        private String destination;
    }

}
