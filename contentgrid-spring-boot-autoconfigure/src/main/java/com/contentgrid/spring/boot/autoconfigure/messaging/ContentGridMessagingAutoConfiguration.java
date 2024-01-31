package com.contentgrid.spring.boot.autoconfigure.messaging;


import java.util.Collection;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.MessageSendingOperations;

@AutoConfiguration(after = {
        RabbitAutoConfiguration.class
})
@ConditionalOnClass({RabbitTemplate.class, MessageSendingOperations.class})
public class ContentGridMessagingAutoConfiguration {

    @Bean
    @ContentGridMessaging
    @ConditionalOnSingleCandidate(RabbitTemplate.class)
    MessageSendingOperations<String> contentGridRabbitMessagingTemplate(
            RabbitTemplate rabbitTemplate,
            @ContentGridMessaging
            Collection<MessageConverter> messageConverters
    ) {
        var messagingTemplate = new RabbitMessagingTemplate(rabbitTemplate);
        if (!messageConverters.isEmpty()) {
            messagingTemplate.setMessageConverter(new CompositeMessageConverter(messageConverters));
        }
        return messagingTemplate;
    }


}
