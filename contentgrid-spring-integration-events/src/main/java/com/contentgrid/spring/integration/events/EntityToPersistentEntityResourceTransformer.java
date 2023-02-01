package com.contentgrid.spring.integration.events;

import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.DataEntity;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessagePayload;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessagePayload.PersistentEntityResourceData;

public class EntityToPersistentEntityResourceTransformer
        extends AbstractPayloadTransformer<ContentGridMessage, ContentGridMessagePayload> {
    private final ContentGridHalAssembler contentGridHalAssembler;

    public EntityToPersistentEntityResourceTransformer(
            ContentGridHalAssembler contentGridHalAssembler) {
        this.contentGridHalAssembler = contentGridHalAssembler;
    }

    @Override
    protected ContentGridMessagePayload transformPayload(ContentGridMessage contentGridMessage) {
        DataEntity updatedEntity = (DataEntity) contentGridMessage.getData();
        PersistentEntityResource newModel = updatedEntity.entity != null
                ? contentGridHalAssembler.toModel(updatedEntity.entity)
                : null;
        PersistentEntityResource oldModel = updatedEntity.old != null
                ? contentGridHalAssembler.toModel(updatedEntity.old)
                : null;

        return new ContentGridMessagePayload(contentGridMessage.getApplicationId(), contentGridMessage.getDeploymentId(),                 
                contentGridMessage.getType(), contentGridMessage.getEntityName(), new PersistentEntityResourceData(oldModel, newModel));
    }
}
