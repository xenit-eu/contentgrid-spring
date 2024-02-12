package com.contentgrid.spring.boot.autoconfigure.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Delegates message conversion to a composite MessageConverter, but adds applicationId and deploymentId to the headers
 */
@RequiredArgsConstructor
public class ContentGridSourceHeadersMessageConverter implements SmartMessageConverter {

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
        var map = new HashMap<>(Objects.requireNonNullElseGet(headers, Map::of));
        if (systemProperties.getApplicationId() != null) {
            map.put("applicationId", systemProperties.getApplicationId());
        }
        if (systemProperties.getDeploymentId() != null) {
            map.put("deploymentId", systemProperties.getDeploymentId());
        }
        return new MessageHeaders(map);
    }
}
