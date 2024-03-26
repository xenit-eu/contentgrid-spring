package com.contentgrid.spring.boot.autoconfigure.audit;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.MessageSendingOperations;

@AutoConfiguration(
        after = ContentGridMessagingAutoConfiguration.class,
        before = ContentGridAuditLoggingAutoConfiguration.class
)
@ConditionalOnClass({MessageSendingOperations.class, MessageSendingAuditHandler.class})
@ConditionalOnBean(value = MessageSendingOperations.class, annotation = ContentGridMessaging.class)
@EnableConfigurationProperties(ContentGridAuditMessagingProperties.class)
@ConditionalOnProperty(prefix = ContentGridAuditMessagingAutoConfiguration.CONTENTGRID_AUDIT_MESSAGING, name = "enabled", matchIfMissing = true)
@Slf4j
public class ContentGridAuditMessagingAutoConfiguration {

    public static final String CONTENTGRID_AUDIT_MESSAGING = "contentgrid.audit.messaging";

    @Bean
    @ConditionalOnProperty(prefix = CONTENTGRID_AUDIT_MESSAGING, name = "destination")
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
    @ConditionalOnProperty(prefix = CONTENTGRID_AUDIT_MESSAGING, name = "source")
    @ConditionalOnClass({CloudEvent.class, AuditEventToCloudEventMessageConverter.class,
            CloudEventMessageConverter.class})
    @Order(10)
    MessageConverter auditEventToCloudEventMessageConverter(
            ObjectMapper objectMapper,
            ContentGridAuditMessagingProperties auditProperties,
            ExpressionParser expressionParser
    ) {
        var parsedSource = expressionParser.parseExpression(auditProperties.getSource()).getValue(URI.class);
        return new AuditEventToCloudEventMessageConverter(
                new CloudEventMessageConverter(),
                objectMapper::writeValueAsBytes,
                parsedSource != null ? parsedSource : URI.create(auditProperties.getSource())
        );
    }

    @Bean
    @ContentGridMessaging
    MessageConverter auditEventMessageConverter(
            ObjectMapper objectMapper
    ) {
        return new AuditEventMessageConverter(objectMapper);
    }

    @ConfigurationProperties(CONTENTGRID_AUDIT_MESSAGING)
    @Data
    static class ContentGridAuditMessagingProperties {

        private String source;

        private String destination;
    }

}
