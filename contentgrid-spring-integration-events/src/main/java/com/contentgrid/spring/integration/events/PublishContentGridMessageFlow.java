package com.contentgrid.spring.integration.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

@RequiredArgsConstructor
public class PublishContentGridMessageFlow implements IntegrationFlow {

    private final ContentGridEventHandlerProperties properties;
    private final EntityChangeEventTransformer toPersistentEntityTransformer;
    private final ObjectMapper halObjectMapper;
    private final List<EntityChangeEventHandler> handlers;

    @Override
    public void configure(IntegrationFlowDefinition<?> flow) {
        flow.transform(toPersistentEntityTransformer)
                .enrichHeaders(Map.of(
                        "application_id", properties.getSystem().getApplicationId(),
                        "deployment_id", properties.getSystem().getDeploymentId(),
                        "webhookConfigUrl", properties.getEvents().getWebhookConfigUrl())
                )
                .transform(
                        Transformers.toJson(new Jackson2JsonObjectMapper(halObjectMapper), MediaTypes.HAL_JSON_VALUE));

        handlers.forEach(handler -> flow.handle(handler.get()));
    }
}
