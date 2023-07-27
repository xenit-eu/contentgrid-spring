package com.contentgrid.spring.data.rest.affordances;

import com.contentgrid.spring.data.rest.webmvc.DomainTypeToHalFormsPayloadMetadataConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * Add default &amp; delete affordances to all places where a self-link is generated for a domain object
 */
@RequiredArgsConstructor
public class AffordanceInjectingSelfLinkProvider implements SelfLinkProvider {
    private final SelfLinkProvider delegate;
    private final ObjectFactory<DomainTypeToHalFormsPayloadMetadataConverter> domainTypeToHalFormsPayloadMetadataConverter;

    @Override
    public Link createSelfLinkFor(Object instance) {
        return addForms(delegate.createSelfLinkFor(instance), instance.getClass());
    }

    @Override
    public Link createSelfLinkFor(Class<?> type, Object reference) {
        return addForms(delegate.createSelfLinkFor(type, reference), type);
    }

    private Link addForms(Link selfLink, Class<?> type) {
        return Affordances.of(selfLink)
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .withInput(domainTypeToHalFormsPayloadMetadataConverter.getObject().convertToUpdatePayloadMetadata(type))
                .withTarget(selfLink)
                .andAfford(HttpMethod.DELETE)
                .withName("delete")
                .build()
                .toLink();
    }
}
