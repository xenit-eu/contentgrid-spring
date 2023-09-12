package com.contentgrid.spring.integration.events;

import java.util.HashMap;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

@MessagingGateway
public interface EntityChangeEventPublisher {

    String CHANGE_EVENT_CHANNEL = "contentgrid.events.channel";

    @Gateway(requestChannel = CHANGE_EVENT_CHANNEL)
    void publish(@NonNull Message<EntityChangeEvent> model);

    default void publish(@NonNull EntityChangeEventPublisher.EntityChangeEvent entityChangeEvent) {
        HashMap<String, Object> headers = new HashMap<>();

        headers.put("trigger", entityChangeEvent.getTrigger().name());
        headers.put("entity", entityChangeEvent.getDomainType().getName());

        publish(new GenericMessage<>(entityChangeEvent, headers));
    }

    @Value
    @Builder
    class EntityChangeEvent {

        @NonNull
        ChangeKind trigger;
        @NonNull
        Class<?> domainType;

        Object oldEntity;
        Object newEntity;

        public Optional<Object> getOldEntity() {
            return Optional.ofNullable(oldEntity);
        }

        public Optional<Object> getNewEntity() {
            return Optional.ofNullable(newEntity);
        }

        public enum ChangeKind {
            create, update, delete
        }
    }

}
