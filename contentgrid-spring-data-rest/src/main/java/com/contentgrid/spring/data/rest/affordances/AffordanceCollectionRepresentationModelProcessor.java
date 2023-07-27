package com.contentgrid.spring.data.rest.affordances;

import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpMethod;

/**
 * Adds an affordance that won't be rendered to {@link CollectionModel}, so {@link org.springframework.data.rest.webmvc.config.HalFormsAdaptingResponseBodyAdvice}
 * does not complain about an unsupported mediatype when requesting collections with only the <code>accept: application/prs.hal-forms+json</code> header
 */
@RequiredArgsConstructor
public class AffordanceCollectionRepresentationModelProcessor implements RepresentationModelProcessor<CollectionModel<?>> {

    @Override
    public CollectionModel<?> process(CollectionModel<?> model) {
        return model.mapLink(IanaLinkRelations.SELF, link -> {
            return Affordances.of(link)
                    .afford(HttpMethod.GET)
                    .toLink();
        });
    }
}
