package com.contentgrid.spring.data.rest.automation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/.contentgrid/automations")
@RequiredArgsConstructor
public class AutomationsRestController {

    private final AutomationsModel model;
    @NonNull
    private final AutomationRepresentationModelAssembler assembler;

    public AutomationsRestController(Resource resource, AutomationRepresentationModelAssembler assembler) {
        this.assembler = assembler;
        if (resource.exists()) {
            try {
                @NonNull ObjectMapper objectMapper = new ObjectMapper()
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                this.model = objectMapper.readValue(resource.getInputStream(), AutomationsModel.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            this.model = AutomationsModel.builder().automations(List.of()).build();
        }
    }

    @GetMapping
    public ResponseEntity<CollectionModel<AutomationRepresentationModel>> getAutomations() {
        var automations = model.getAutomations();

        var collection = assembler.toCollectionModel(automations);

        var result = ResponseEntity.ok(collection);
        return result;
    }

    @GetMapping("{id}")
    public ResponseEntity<AutomationRepresentationModel> getAutomation(@PathVariable String id) {
        var automation = model.getAutomations().stream()
                .filter(aut -> aut.getId().equals(id))
                .findFirst();

        var result = automation.map(aut -> assembler.toModel(aut, true))
                .map(ResponseEntity::ok);

        return result.orElseGet(() -> ResponseEntity.notFound().build());
    }

}
