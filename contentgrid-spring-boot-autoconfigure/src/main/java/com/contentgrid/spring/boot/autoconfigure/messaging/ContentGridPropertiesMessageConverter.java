package com.contentgrid.spring.boot.autoconfigure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Delegates message conversion to a composite MessageConverter, but adds applicationId and deploymentId to the headers
 */
@RequiredArgsConstructor
public class ContentGridPropertiesMessageConverter implements SmartMessageConverter {

    private final SmartMessageConverter delegate;
    private final ContentGridMessagingSystemProperties systemProperties;

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint) {
        return delegate.fromMessage(message, targetClass, conversionHint);
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
        return delegate.toMessage(payload, extendHeaders(headers), conversionHint);
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        return delegate.fromMessage(message, targetClass);
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        return delegate.toMessage(payload, extendHeaders(headers));
    }

    private MessageHeaders extendHeaders(MessageHeaders headers) {
        return MessageBuilder.withPayload("foo").copyHeaders(headers)
                .setHeaderIfAbsent("applicationId", systemProperties.getApplicationId())
                .setHeaderIfAbsent("deploymentId", systemProperties.getDeploymentId())
                .build()
                .getHeaders();
    }
}
