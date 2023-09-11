package com.contentgrid.spring.integration.events;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageTrigger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.hateoas.EntityModel;
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
            final Object _new;

            DataEntity(Object old, Object _new) {
                this.old = old;
                this._new = _new;
            }

            public Object getNew() {
                return _new;
            }

            public Object getOld() {
                return old;
            }
        }
    }

    @RequiredArgsConstructor
    class ContentGridMessagePayload {

        private final ContentGridMessageTrigger trigger;
        private final PersistentEntityResourceData data;

        @JsonUnwrapped
        public PersistentEntityResourceData getData() {
            return data;
        }

        public ContentGridMessageTrigger getTrigger() {
            return trigger;
        }

        @Value
        static class PersistentEntityResourceData {
            EntityModel<?> old;
            @JsonProperty("new")
            EntityModel<?> _new;
        }
    }
}
