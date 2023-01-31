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

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageTrigger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@MessagingGateway
public interface ContentGridEventPublisher {

    public static final String CONTENTGRID_EVENT_CHANNEL = "contentgrid.events.channel";

    @Gateway(requestChannel = CONTENTGRID_EVENT_CHANNEL)
    void publish(Message<ContentGridMessage> model);

    default void publish(ContentGridMessage contentGridMessage) {
        Assert.notNull(contentGridMessage, "contentGridMessage cannot be null");
        Assert.hasText(contentGridMessage.applicationId,
                "contentGridMessage.application cannot be empty");
        Assert.notNull(contentGridMessage.trigger, "contentGridMessage.type cannot be null");
        Assert.notNull(contentGridMessage.data, "contentGridMessage.data cannot be null");

        HashMap<String, Object> headers = new HashMap<>();
        if (!ObjectUtils.isEmpty(contentGridMessage.headers)) {
            headers.putAll(contentGridMessage.headers);
        }

        headers.put("application_id", contentGridMessage.applicationId);
        headers.put("deployment_id", contentGridMessage.deploymentId);
        headers.put("trigger", contentGridMessage.trigger);
        headers.put("entity", contentGridMessage.getEntityName());

        publish(new GenericMessage<>(contentGridMessage, headers));
    }
    

    static class ContentGridMessage {
        private final String applicationId;
        private final String deploymentId;
        private final ContentGridMessageTrigger trigger;
        private final DataEntity data;
        private final Map<String, Object> headers;
        private final String entityName;

        public ContentGridMessage(String applicationId, String deploymentId, ContentGridMessageTrigger type, DataEntity data, 
                String entityName ) {
            this.applicationId = applicationId;
            this.deploymentId = deploymentId;
            this.trigger = type;
            this.data = data;
            this.headers = Collections.emptyMap();
            this.entityName = entityName;
        }

        public ContentGridMessage(String applicationId, String deploymentId, ContentGridMessageTrigger type, DataEntity data,
                String entityName, Map<String, Object> headers) {
            this.applicationId = applicationId;
            this.deploymentId = deploymentId;
            this.trigger = type;
            this.data = data;
            this.headers = headers;
            this.entityName = entityName;
        }
        
        public String getEntityName() {
            return entityName;
        }

        public String getApplicationId() {
            return applicationId;
        }
        
        public String getDeploymentId() {
            return deploymentId;
        }

        public DataEntity getData() {
            return data;
        }

        public ContentGridMessageTrigger getType() {
            return trigger;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public static enum ContentGridMessageTrigger {
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
        private final String applicationId;
        private final String deploymentId;
        private final ContentGridMessageTrigger trigger;
        private final PersistentEntityResourceData data;
        private final String entityName;

        public ContentGridMessagePayload(String applicationId, String deploymentId, ContentGridMessageTrigger type,
                String entityName, PersistentEntityResourceData data) {
            this.applicationId = applicationId;
            this.deploymentId = deploymentId;
            this.trigger = type;
            this.data = data;
            this.entityName = entityName;
        }

        @JsonProperty("entity")
        public String getEntityName() {
            return entityName;
        }

        @JsonProperty("application_id")
        public String getApplicationId() {
            return applicationId;
        }
        
        @JsonProperty("deployment_id")
        public String getDeploymentId() {
            return deploymentId;
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
