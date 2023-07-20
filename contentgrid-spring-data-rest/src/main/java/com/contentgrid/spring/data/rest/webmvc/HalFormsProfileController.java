package com.contentgrid.spring.data.rest.webmvc;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequiredArgsConstructor
@BasePathAwareController
public class HalFormsProfileController {

    private final RepositoryRestConfiguration configuration;
    private final EntityLinks entityLinks;
    private final DomainTypeToHalFormsPayloadMetadataConverter toHalFormsPayloadMetadataConverter;

    @RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = RequestMethod.OPTIONS, produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    HttpEntity<?> halFormsOptions() {
        var headers = new HttpHeaders();

        headers.setAllow(Collections.singleton(HttpMethod.GET));

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    @RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = RequestMethod.GET, produces = {
            MediaTypes.HAL_FORMS_JSON_VALUE
    })
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    RepresentationModel<?> halFormsProfile(RootResourceInformation information) {
        var model = new RepresentationModel<>();

        model.add(Link.of(ProfileController.getPath(configuration, information.getResourceMetadata())));

        var collectionLink = entityLinks.linkFor(information.getDomainType()).withRel("collection");

        var collectionAffordances = Affordances.of(collectionLink)
                .afford(HttpMethod.HEAD) // This is to pin down the default affordance, which we don't care about
                .andAfford(HttpMethod.POST)
                .withName("create")
                .withInput(toHalFormsPayloadMetadataConverter.convertToCreatePayloadMetadata(information.getDomainType()))
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .build();

        model.add(collectionAffordances.toLink());

        return model;
    }

}
