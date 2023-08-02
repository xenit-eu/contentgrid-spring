package com.contentgrid.spring.data.rest.links;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.RepresentationModelProcessor;

@RequiredArgsConstructor
public class SpringDataRepositoryLinksResourceProcessor implements RepresentationModelProcessor<RepositoryLinksResource> {
    private final Repositories repositories;
    private final ResourceMappings mappings;
    private final EntityLinks entityLinks;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource model) {
        for (Class<?> domainType : repositories) {
            var metadata = mappings.getMetadataFor(domainType);
            if(metadata.isExported()) {
                var collectionLink = entityLinks.linkToCollectionResource(domainType);

                model.add(
                        collectionLink
                                .withRel(ContentGridLinkRelations.ENTITY)
                                .withName(HalLinkRelation.of(collectionLink.getRel()).getLocalPart())
                );

            }

        }
        return model;
    }
}
