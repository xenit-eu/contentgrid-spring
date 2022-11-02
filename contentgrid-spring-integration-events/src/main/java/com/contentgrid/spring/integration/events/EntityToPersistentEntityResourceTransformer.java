package com.contentgrid.spring.integration.events;

import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

public class EntityToPersistentEntityResourceTransformer
        extends AbstractPayloadTransformer<Object, PersistentEntityResource> {
    private final ContentGridHalAssembler contentGridHalAssembler;

    public EntityToPersistentEntityResourceTransformer(
            ContentGridHalAssembler contentGridHalAssembler) {
        this.contentGridHalAssembler = contentGridHalAssembler;
    }

    @Override
    protected PersistentEntityResource transformPayload(Object entity) {
        return contentGridHalAssembler.toModel(entity);
    }
}
