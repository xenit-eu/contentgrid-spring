package com.contentgrid.automations.rest;

import com.contentgrid.automations.AutomationLinkRelations;
import com.contentgrid.automations.rest.AutomationsModel.AutomationAnnotationModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationAnnotationRepresentationModelAssembler implements
        RepresentationModelAssembler<AutomationAnnotationModel, AutomationAnnotationRepresentationModel> {

    private final RepositoryRestConfiguration repositoryRestConfiguration;
    private final ResourceMappings mappings;

    @Override
    public AutomationAnnotationRepresentationModel toModel(AutomationAnnotationModel annotation) {
        return AutomationAnnotationRepresentationModel.from(annotation)
                .add(getTargetEntityLink(annotation));
    }

    private Link getTargetEntityLink(AutomationAnnotationModel annotation) {
        var profileUrl = ProfileController.getPath(repositoryRestConfiguration,
                mappings.getMetadataFor(annotation.getEntityClass()));

        return Link.of(profileUrl, AutomationLinkRelations.TARGET_ENTITY);
    }
}
