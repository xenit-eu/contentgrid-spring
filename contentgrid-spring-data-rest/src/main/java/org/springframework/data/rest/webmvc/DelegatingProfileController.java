package org.springframework.data.rest.webmvc;

import static org.springframework.data.rest.webmvc.ProfileController.PROFILE_ROOT_MAPPING;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.contentgrid.spring.data.rest.webmvc.ProfileLinksResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@BasePathAwareController
@RequiredArgsConstructor
public class DelegatingProfileController {
    private final ProfileController delegate;

    @RequestMapping(value = PROFILE_ROOT_MAPPING, method = RequestMethod.OPTIONS)
    public HttpEntity<?> profileOptions() {
        return delegate.profileOptions();
    }

    @RequestMapping(value = PROFILE_ROOT_MAPPING, method = GET)
    public HttpEntity<ProfileLinksResource> listAllFormsOfMetadata() {
        var resource = new ProfileLinksResource();
        var originalModel = delegate.listAllFormsOfMetadata();

        // The change from the spring-data-rest ProfileController is returning a specific ProfileLinksResource.
        // This specific resource class can be used by a RepresentationModelProcessor to further enhance the response.
        resource.add(originalModel.getBody().getLinks().toList());

        return ResponseEntity.ok()
                .headers(originalModel.getHeaders())
                .body(resource);
    }

}
