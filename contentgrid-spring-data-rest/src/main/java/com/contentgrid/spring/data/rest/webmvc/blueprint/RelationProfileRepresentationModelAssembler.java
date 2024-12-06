package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.webmvc.blueprint.config.RelationModel;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelationProfileRepresentationModelAssembler implements
        RepresentationModelAssembler<RelationModel, RelationProfileRepresentationModel> {

    private final RepositoryRestConfiguration repositoryRestConfiguration;
    private final ResourceMappings mappings;

    @Override
    public RelationProfileRepresentationModel toModel(@NonNull RelationModel relation) {
        var targetLink = getTargetEntityLink(relation);
        return RelationProfileRepresentationModel.from(relation)
                .addIf(targetLink.isPresent(), targetLink::get);
    }

    private Optional<Link> getTargetEntityLink(RelationModel relation) {
        try {
            var domainType = Class.forName(relation.getTargetEntity());
            var profileUrl = ProfileController.getPath(repositoryRestConfiguration,
                    mappings.getMetadataFor(domainType));

            return Optional.of(Link.of(profileUrl, BlueprintLinkRelations.TARGET_ENTITY));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
