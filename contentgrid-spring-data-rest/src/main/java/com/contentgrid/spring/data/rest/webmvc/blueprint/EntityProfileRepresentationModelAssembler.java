package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.webmvc.blueprint.config.EntityModel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityProfileRepresentationModelAssembler implements
        RepresentationModelAssembler<EntityModel, EntityProfileRepresentationModel> {

    private final RelationProfileRepresentationModelAssembler relationAssembler;

    @Override
    public EntityProfileRepresentationModel toModel(@NonNull EntityModel entity) {
        var attributes = entity.getAttributes().stream()
                .map(AttributeProfileRepresentationModel::from)
                .toList();
        var relations = entity.getRelations().stream()
                .map(relationAssembler::toModel)
                .toList();

        return EntityProfileRepresentationModel.builder()
                .name(entity.getName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .attributes(attributes)
                .relations(relations)
                .build();
    }
}
