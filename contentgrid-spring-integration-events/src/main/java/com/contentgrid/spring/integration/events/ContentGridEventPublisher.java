package com.contentgrid.spring.integration.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@MessagingGateway
public interface ContentGridEventPublisher {

    public static final String CONTENTGRID_EVENT_CHANNEL = "contentgrid.events.channel";

    @Gateway(requestChannel = CONTENTGRID_EVENT_CHANNEL)
    void publish(Message<ContentGridMessage> model);

    default void publish(ContentGridMessage contentGridMessage) {
        Assert.notNull(contentGridMessage, "contentGridMessage cannot be null");
        Assert.hasText(contentGridMessage.application,
                "contentGridMessage.application cannot be empty");
        Assert.notNull(contentGridMessage.type, "contentGridMessage.type cannot be null");
        Assert.notNull(contentGridMessage.data, "contentGridMessage.data cannot be null");

        HashMap<String, Object> headers = new HashMap<>();
        if (!ObjectUtils.isEmpty(contentGridMessage.headers)) {
            headers.putAll(contentGridMessage.headers);
        }

        headers.put("application", contentGridMessage.application);
        headers.put("type", contentGridMessage.type);

        publish(new GenericMessage<>(contentGridMessage, headers));
    }

    static class ContentGridMessage {
        private final String application;
        private final ContentGridMessageType type;
        private final DataEntity data;
        private final Map<String, Object> headers;
        private final Class<?> entity;

        public ContentGridMessage(String application, ContentGridMessageType type, Class<?> entity,
                DataEntity data) {
            this.application = application;
            this.type = type;
            this.data = data;
            this.headers = Collections.emptyMap();
            this.entity = entity;
        }

        public ContentGridMessage(String application, ContentGridMessageType type, DataEntity data,
                Class<?> entity, Map<String, Object> headers) {
            this.application = application;
            this.type = type;
            this.data = data;
            this.headers = headers;
            this.entity = entity;
        }
        
        public Class<?> getEntity() {
            return entity;
        }

        public String getApplication() {
            return application;
        }

        public DataEntity getData() {
            return data;
        }

        public ContentGridMessageType getType() {
            return type;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public static enum ContentGridMessageType {
            create, update, delete
        }

        static class DataEntity {
            final Object old;
            final Object entity;

            DataEntity(Object old, Object entity) {
                this.old = old;
                this.entity = entity;
            }

            public Object getEntity() {
                return entity;
            }

            public Object getOld() {
                return old;
            }
        }
    }

    static class ContentGridMessagePayload {
        private final String application;
        private final ContentGridMessageType type;
        private final PersistentEntityResourceData data;
        private final Class<?> entity;

        public ContentGridMessagePayload(String application, ContentGridMessageType type,
                Class<?> entity, PersistentEntityResourceData data) {
            this.application = application;
            this.type = type;
            this.data = data;
            this.entity = entity;
        }

        public Class<?> getEntity() {
            return entity;
        }

        public String getApplication() {
            return application;
        }

        @JsonUnwrapped
        public PersistentEntityResourceData getData() {
            return data;
        }

        public ContentGridMessageType getType() {
            return type;
        }

        static class PersistentEntityResourceData {
            final PersistentEntityResource old;
            final PersistentEntityResource entity;

            PersistentEntityResourceData(PersistentEntityResource old,
                    PersistentEntityResource entity) {
                this.old = old;
                this.entity = entity;
            }

            @JsonProperty("new")
            public Object getEntity() {
                return entity;
            }

            public Object getOld() {
                return old;
            }
        }
    }
}
