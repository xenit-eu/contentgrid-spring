package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.handler.amqp.AuditEventMessageConverter;
import com.contentgrid.spring.audit.handler.amqp.AmqpAuditHandler;
import com.contentgrid.spring.audit.handler.amqp.AuditEventToCloudEventMessageConverter;
import com.contentgrid.spring.boot.autoconfigure.audit.ContentGridAmqpAuditAutoConfiguration.ContentGridAuditAmqpProperties;
import com.contentgrid.spring.boot.autoconfigure.cloudevents.CloudEventsAmqpAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import java.net.URI;
import lombok.Data;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

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
    @Conditional(AuditCloudEventsCondition.class)
    RabbitTemplateCustomizer auditEventToCloudEventMessageConverterRabbitTemplateCustomizer(
            ObjectProvider<ObjectMapper> objectMapper,
            ContentGridAuditAmqpProperties auditProperties
    ) {
        return rabbitTemplate -> {
            var mapper = objectMapper.getIfAvailable(ObjectMapper::new);
            var originalConverter = rabbitTemplate.getMessageConverter();
            var newConverter = new AuditEventToCloudEventMessageConverter(originalConverter,
                    mapper::writeValueAsBytes, auditProperties.getSource());
            rabbitTemplate.setMessageConverter(newConverter);
        };
    }

    @Bean
    @Conditional(NoAuditCloudEventsCondition.class)
    RabbitTemplateCustomizer auditEventMessageConverterRabbitTemplateCustomizer(
            ObjectProvider<ObjectMapper> objectMapper
    ) {
        return rabbitTemplate -> {
            var originalConverter = rabbitTemplate.getMessageConverter();
            var newConverter = new AuditEventMessageConverter(objectMapper.getIfAvailable(ObjectMapper::new),
                    originalConverter);
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

    private static class AuditCloudEventsCondition extends AllNestedConditions {

        AuditCloudEventsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty("contentgrid.audit.amqp.source")
        static class AmqpSourceProperty {

        }

        @ConditionalOnClass({CloudEvent.class, AuditEventToCloudEventMessageConverter.class})
        static class CloudEventClassesAvailable {

        }
    }

    private static class NoAuditCloudEventsCondition extends NoneNestedConditions {

        NoAuditCloudEventsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @Conditional(AuditCloudEventsCondition.class)
        static class AuditCloudEvents {

        }
    }
}
