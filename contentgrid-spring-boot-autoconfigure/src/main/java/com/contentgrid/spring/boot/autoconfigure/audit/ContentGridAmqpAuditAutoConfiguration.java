package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.handler.amqp.AuditEventToCloudEventMessageConverter;
import com.contentgrid.spring.audit.handler.amqp.AmqpAuditHandler;
import com.contentgrid.spring.boot.autoconfigure.audit.ContentGridAmqpAuditAutoConfiguration.ContentGridAuditAmqpProperties;
import com.contentgrid.spring.boot.autoconfigure.cloudevents.CloudEventsAmqpAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import java.net.URI;
import lombok.Data;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {
        RabbitAutoConfiguration.class,
        CloudEventsAmqpAutoConfiguration.class
}, before = {
        ContentGridAuditLoggingAutoConfiguration.class
})
@ConditionalOnClass({AmqpTemplate.class, AmqpAuditHandler.class})
@ConditionalOnBean(AmqpTemplate.class)
@EnableConfigurationProperties(ContentGridAuditAmqpProperties.class)
@ConditionalOnProperty(value = "contentgrid.audit.amqp.enabled", matchIfMissing = true)
public class ContentGridAmqpAuditAutoConfiguration {

    @Bean
    AmqpAuditHandler amqpAuditHandler(AmqpTemplate amqpTemplate, ContentGridAuditAmqpProperties auditProperties) {
        return new AmqpAuditHandler(
                amqpTemplate,
                auditProperties.getExchange(),
                auditProperties.getRoutingKey()
        );
    }

    @Bean
    @ConditionalOnClass({CloudEvent.class, AuditEventToCloudEventMessageConverter.class})
    @ConditionalOnProperty("contentgrid.audit.amqp.source")
    RabbitTemplateCustomizer auditEventMessageConverterRabbitTemplateCustomizer(
            ObjectMapper objectMapper,
            ContentGridAuditAmqpProperties auditProperties
    ) {
        return rabbitTemplate -> {
            var originalConverter = rabbitTemplate.getMessageConverter();
            var newConverter = new AuditEventToCloudEventMessageConverter(originalConverter,
                    objectMapper::writeValueAsBytes, auditProperties.getSource());
            rabbitTemplate.setMessageConverter(newConverter);
        };
    }

    @ConfigurationProperties("contentgrid.audit.amqp")
    @Data
    static class ContentGridAuditAmqpProperties {

        private URI source;

        private String routingKey;

        private String exchange;
    }

}
