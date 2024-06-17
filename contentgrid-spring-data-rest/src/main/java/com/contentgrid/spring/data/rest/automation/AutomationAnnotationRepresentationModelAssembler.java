package com.contentgrid.spring.data.rest.automation;

import com.contentgrid.spring.data.rest.automation.AutomationsModel.AutomationAnnotationModel;
import com.contentgrid.spring.data.rest.links.ContentGridLinkRelations;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationAnnotationRepresentationModelAssembler implements
        RepresentationModelAssembler<AutomationAnnotationModel, AutomationAnnotationRepresentationModel> {

    private final RepositoryRestConfiguration repositoryRestConfiguration;
    private final RepositoryResourceMappings mappings;
    private final RepositoryEntityLinks entityLinks;

    @Override
    public AutomationAnnotationRepresentationModel toModel(AutomationAnnotationModel annotation) {
        return AutomationAnnotationRepresentationModel.from(annotation)
                .add(getProfileLink(annotation))
                .add(getCollectionLink(annotation));
    }

    private Link getProfileLink(AutomationAnnotationModel annotation) {
        var mapping = mappings.getMetadataFor(annotation.getEntityClass());
        return Link.of(ProfileController.getPath(repositoryRestConfiguration, mapping),
                ContentGridLinkRelations.ENTITY_PROFILE);
    }

    private Link getCollectionLink(AutomationAnnotationModel annotation) {
        return entityLinks.linkToCollectionResource(annotation.getEntityClass()).withRel(ContentGridLinkRelations.ENTITY);
    }
}
