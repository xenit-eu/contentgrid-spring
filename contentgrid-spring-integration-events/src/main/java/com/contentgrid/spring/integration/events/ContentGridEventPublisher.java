package com.contentgrid.spring.integration.events;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

@MessagingGateway
public interface ContentGridEventPublisher {

    public static final String CONTENTGRID_EVENT_CHANNEL = "contentgrid.events.channel";

    @Gateway(requestChannel = CONTENTGRID_EVENT_CHANNEL)
    void publish(Message<Object> model);

    default void publish(ContentGridMessage contentGridMessage) {
        Assert.notNull(contentGridMessage, "contentGridMessage cannot be null");
        Assert.hasText(contentGridMessage.application,
                "contentGridMessage.application cannot be empty");
        Assert.notNull(contentGridMessage.type, "contentGridMessage.type cannot be null");
        Assert.notNull(contentGridMessage.entity, "contentGridMessage.entity cannot be null");

        HashMap<String, Object> headers = new HashMap<>();
        if (!ObjectUtils.isEmpty(contentGridMessage.headers)) {
            headers.putAll(contentGridMessage.headers);
        }

        headers.put("application", contentGridMessage.application);
        headers.put("type", contentGridMessage.type);

        publish(new GenericMessage<>(contentGridMessage.entity, headers));
    }

    static class ContentGridMessage {
        private String application;
        private ContentGridMessageType type;
        private Object entity;
        private Map<String, Object> headers;

        public ContentGridMessage application(String application) {
            Assert.hasText(application, "application cannot be empty");
            this.application = application;
            return this;
        }

        public ContentGridMessage type(ContentGridMessageType type) {
            Assert.notNull(type, "type cannot be null");
            this.type = type;
            return this;
        }

        public ContentGridMessage entity(Object entity) {
            Assert.notNull(entity, "entity cannot be null");
            this.entity = entity;
            return this;
        }

        public ContentGridMessage headers(Map<String, Object> headers) {
            Assert.notNull(headers, "headers cannot be null");
            Assert.notEmpty(headers, "headers cannot be empty");
            this.headers = headers;
            return this;
        }

        public ContentGridMessage builder() {
            return new ContentGridMessage();
        }

        public static enum ContentGridMessageType {
            create, update, delete
        }
    }
}
