package com.contentgrid.spring.data.rest.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;

/**
 * Collects links to jpa relations in the {@link ContentGridLinkRelations#RELATION} link-relation
 */
@RequiredArgsConstructor
class SpringDataAssociationLinkCollector implements ContentGridLinkCollector {
    private final PersistentEntities entities;
    private final Associations associationLinks;
    private final SelfLinkProvider selfLinkProvider;

    @Override
    public Links getLinksFor(Object object, Links existing) {
        // Logic based on the DefaultLinkCollector from spring-data-rest
        var selfLink = selfLinkProvider.createSelfLinkFor(object);

        if(selfLink == null) {
            return existing;
        }

        Path selfPath = new Path(selfLink.expand().getHref());

        var links = new ArrayList<Link>();

        entities.getRequiredPersistentEntity(object.getClass()).doWithAssociations(
                (SimpleAssociationHandler) association -> {
                    if(!associationLinks.isLinkableAssociation(association)) {
                        return;
                    }

                    var property = association.getInverse();
                    var linkName = readLinkName(property);

                    for (Link link : associationLinks.getLinksFor(association, selfPath)) {
                        links.add(link.withRel(ContentGridLinkRelations.RELATION).withName(linkName));
                    }
                });

        return existing.and(links);
    }

    @Override
    public Links getLinksForNested(Object object, Links existing) {
        return existing;
    }

    private String readLinkName(PersistentProperty<?> property) {
        var jsonProperty = property.findAnnotation(JsonProperty.class);
        if(jsonProperty != null && !Objects.equals(jsonProperty.value(), JsonProperty.USE_DEFAULT_NAME)) {
            return jsonProperty.value();
        }
        return property.getName();
    }

}
