package com.contentgrid.spring.integration.events;

import com.contentgrid.spring.integration.events.EntityChangeEventPublisher.EntityChangeEvent;
import com.contentgrid.spring.integration.events.EntityChangeEventTransformer.ChangeEventPayload;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

@RequiredArgsConstructor
public class EntityChangeEventTransformer extends AbstractPayloadTransformer<EntityChangeEvent, ChangeEventPayload> {

    private final EntityModelAssembler entityModelAssembler;
    private final ObjectMapper halObjectMapper;

    @Override
    protected ChangeEventPayload transformPayload(EntityChangeEvent changeEvent) {
        return new ChangeEventPayload(
                changeEvent.getTrigger().toString().toLowerCase(Locale.ROOT),
                changeEvent.getOldEntity()
                        .map(entityModelAssembler::toModel)
                        .<JsonNode>map(halObjectMapper::valueToTree)
                        .orElse(null),
                changeEvent.getNewEntity()
                        .map(entityModelAssembler::toModel)
                        .<JsonNode>map(halObjectMapper::valueToTree)
                        .orElse(null)
        );
    }

    @RequiredArgsConstructor
    public static class ChangeEventPayload {

        @Getter
        @NonNull
        private final String trigger;

        @Getter
        private final JsonNode old;

        @JsonProperty("new")
        private final JsonNode _new;

        public JsonNode getNew() {
            return _new;
        }
    }
}
