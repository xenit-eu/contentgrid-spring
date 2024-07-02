package com.contentgrid.automations.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.contentgrid.automations.rest.AutomationsModel.AutomationModel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationRepresentationModelAssembler implements RepresentationModelAssembler<AutomationModel, AutomationRepresentationModel> {

    @NonNull
    private final AutomationAnnotationRepresentationModelAssembler annotationAssembler;

    @Override
    public AutomationRepresentationModel toModel(AutomationModel automation) {
        return toModel(automation, false);
    }

    public AutomationRepresentationModel toModel(AutomationModel automation, boolean expandAnnotations) {
        AutomationRepresentationModel result;
        if (expandAnnotations) {
            result = AutomationRepresentationModel.expandedFrom(automation, annotationAssembler.toCollectionModel(
                    automation.getAnnotations()));
        } else {
            result = AutomationRepresentationModel.from(automation);
        }

        result.add(linkTo(methodOn(AutomationsRestController.class).getAutomation(automation.getId())).withSelfRel());

        return result;
    }

    @Override
    public CollectionModel<AutomationRepresentationModel> toCollectionModel(Iterable<? extends AutomationModel> automations) {
        var result = RepresentationModelAssembler.super.toCollectionModel(automations);
        result.add(linkTo(methodOn(AutomationsRestController.class).getAutomations()).withSelfRel());
        return result;
    }
}
