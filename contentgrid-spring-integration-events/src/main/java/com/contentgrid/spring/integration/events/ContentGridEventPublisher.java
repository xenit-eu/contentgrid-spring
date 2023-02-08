package com.contentgrid.spring.integration.events;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageTrigger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.HashMap;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

@MessagingGateway
public interface ContentGridEventPublisher {

    public static final String CONTENTGRID_EVENT_CHANNEL = "contentgrid.events.channel";

    @Gateway(requestChannel = CONTENTGRID_EVENT_CHANNEL)
    void publish(Message<ContentGridMessage> model);

    default void publish(ContentGridMessage contentGridMessage) {
        Assert.notNull(contentGridMessage, "contentGridMessage cannot be null");
        Assert.notNull(contentGridMessage.trigger, "contentGridMessage.type cannot be null");
        Assert.notNull(contentGridMessage.data, "contentGridMessage.data cannot be null");

        HashMap<String, Object> headers = new HashMap<>();

        headers.put("trigger", contentGridMessage.getTrigger());
        headers.put("entity", contentGridMessage.getEntityName());

        publish(new GenericMessage<>(contentGridMessage, headers));
    }


    class ContentGridMessage {

        private final ContentGridMessageTrigger trigger;
        private final DataEntity data;
        private final String entityName;

        public ContentGridMessage(ContentGridMessageTrigger type, DataEntity data, String entityName) {
            this.trigger = type;
            this.data = data;
            this.entityName = entityName;
        }

        public String getEntityName() {
            return entityName;
        }

        public DataEntity getData() {
            return data;
        }

        public ContentGridMessageTrigger getTrigger() {
            return trigger;
        }

        public enum ContentGridMessageTrigger {
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

    class ContentGridMessagePayload {

        private final ContentGridMessageTrigger trigger;
        private final PersistentEntityResourceData data;
        private final String entityName;

        public ContentGridMessagePayload(ContentGridMessageTrigger type,
                String entityName, PersistentEntityResourceData data) {
            this.trigger = type;
            this.data = data;
            this.entityName = entityName;
        }

        @JsonProperty("entity")
        public String getEntityName() {
            return entityName;
        }

        @JsonUnwrapped
        public PersistentEntityResourceData getData() {
            return data;
        }

        public ContentGridMessageTrigger getTrigger() {
            return trigger;
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
