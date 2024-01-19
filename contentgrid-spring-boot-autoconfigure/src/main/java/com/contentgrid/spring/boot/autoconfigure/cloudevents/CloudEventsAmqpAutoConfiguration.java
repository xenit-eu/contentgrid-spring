package com.contentgrid.spring.boot.autoconfigure.cloudevents;

import com.contentgrid.spring.cloudevents.amqp.CloudEventMessageConverter;
import io.cloudevents.CloudEvent;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({CloudEventMessageConverter.class, Message.class, CloudEvent.class})
public class CloudEventsAmqpAutoConfiguration {

    @Bean
    RabbitTemplateCustomizer cloudEventsAmqpRabbitTemplateCustomizer() {
        return rabbitTemplate -> {
            var originalConverter = rabbitTemplate.getMessageConverter();
            var cloudEventConverter = new CloudEventMessageConverter(originalConverter);
            rabbitTemplate.setMessageConverter(cloudEventConverter);
        };
    }
}
