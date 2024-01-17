package com.contentgrid.spring.data.rest.links;

import com.contentgrid.spring.data.rest.webmvc.ProfileLinksResource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.hateoas.server.RepresentationModelProcessor;

@RequiredArgsConstructor
public class SpringDataProfileLinksResourceProcessor implements RepresentationModelProcessor<ProfileLinksResource> {
    private final Repositories repositories;
    private final ResourceMappings mappings;
    private final RepositoryRestConfiguration configuration;
    private final MessageResolver resolver;

    @Override
    @SuppressWarnings("ConstantConditions") // .withTitle(...) should be annotated @Nullable
    public ProfileLinksResource process(ProfileLinksResource model) {
        for (Class<?> domainType : repositories) {
            var metadata = mappings.getMetadataFor(domainType);
            if (metadata.isExported()) {
                model.add(
                        Link.of(ProfileController.getPath(configuration, metadata))
                                .withRel(ContentGridLinkRelations.ENTITY)
                                .withName(HalLinkRelation.of(metadata.getRel()).getLocalPart())
                                .withTitle(resolver.resolve(LinkTitles.forEntity(domainType)))
                );

            }

        }
        return model;
    }
}
