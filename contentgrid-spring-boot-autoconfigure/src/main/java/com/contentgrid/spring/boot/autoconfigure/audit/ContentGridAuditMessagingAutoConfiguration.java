package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.handler.messaging.AuditEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.AuditEventToCloudEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.MessageSendingAuditHandler;
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
            ObjectProvider<ObjectMapper> objectMapper,
            ContentGridAuditMessagingProperties auditProperties
    ) {
        var mapper = objectMapper.getIfAvailable(ObjectMapper::new);
        return new AuditEventToCloudEventMessageConverter(
                new CloudEventMessageConverter(),
                mapper::writeValueAsBytes,
                auditProperties.getSource()
        );
    }

    @Bean
    @ContentGrid
    MessageConverter auditEventMessageConverter(
            ObjectProvider<ObjectMapper> objectMapper
    ) {
        return new AuditEventMessageConverter(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @ConfigurationProperties("contentgrid.audit.messaging")
    @Data
    static class ContentGridAuditMessagingProperties {

        private URI source;

        private String destination;
    }

}
