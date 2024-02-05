package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.ContentGridAuditEventConfiguration.ContentgridAuditSystemProperties;
import com.contentgrid.spring.audit.handler.messaging.AuditEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.AuditEventToCloudEventMessageConverter;
import com.contentgrid.spring.audit.handler.messaging.Jackson2AuditMessagingModule;
import com.contentgrid.spring.audit.handler.messaging.MessageSendingAuditHandler;
import com.contentgrid.spring.boot.autoconfigure.audit.ContentGridAuditMessagingAutoConfiguration.ContentGridAuditMessagingProperties;
import com.contentgrid.spring.boot.autoconfigure.messaging.ContentGridMessaging;
import com.contentgrid.spring.boot.autoconfigure.messaging.ContentGridMessagingAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;
import java.net.URI;
import lombok.Data;
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
@ConditionalOnBean(value = MessageSendingOperations.class, annotation = ContentGridMessaging.class)
@EnableConfigurationProperties(ContentGridAuditMessagingProperties.class)
@ConditionalOnProperty(value = "contentgrid.audit.messaging.enabled", matchIfMissing = true)
public class ContentGridAuditMessagingAutoConfiguration {

    @Bean
    MessageSendingAuditHandler messageSendingAuditHandler(
            @ContentGridMessaging MessageSendingOperations<String> sendingOperations,
            ContentGridAuditMessagingProperties auditProperties
    ) {
        return new MessageSendingAuditHandler(sendingOperations, auditProperties.getDestination());
    }

    @Bean
    Jackson2AuditMessagingModule jackson2AuditMessagingModule() {
        return new Jackson2AuditMessagingModule();
    }

    @Bean
    @ContentGridMessaging
    @ConditionalOnProperty("contentgrid.audit.messaging.source")
    @ConditionalOnClass({CloudEvent.class, AuditEventToCloudEventMessageConverter.class,
            CloudEventMessageConverter.class})
    @Order(10)
    MessageConverter auditEventToCloudEventMessageConverter(
            ObjectMapper objectMapper,
            ContentGridAuditMessagingProperties auditProperties
    ) {
        return new AuditEventToCloudEventMessageConverter(
                new CloudEventMessageConverter(),
                objectMapper::writeValueAsBytes,
                auditProperties.getSource()
        );
    }

    @Bean
    @ContentGridMessaging
    MessageConverter auditEventMessageConverter(
            ObjectMapper objectMapper
    ) {
        return new AuditEventMessageConverter(objectMapper);
    }

    @ConfigurationProperties("contentgrid.audit.messaging")
    @Data
    static class ContentGridAuditMessagingProperties {

        private URI source;

        private String destination;
    }

    @Bean
    @ConfigurationProperties(prefix = "contentgrid.system")
    public ContentgridAuditSystemProperties systemProperties() {
        return new ContentgridAuditSystemProperties();
    }

}
