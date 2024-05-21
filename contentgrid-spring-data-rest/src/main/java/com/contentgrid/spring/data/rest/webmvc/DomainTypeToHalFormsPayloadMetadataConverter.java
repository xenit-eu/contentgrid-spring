package com.contentgrid.spring.data.rest.webmvc;

import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.http.MediaType;

public interface DomainTypeToHalFormsPayloadMetadataConverter {
    PayloadMetadata convertToCreatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadata convertToUpdatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadata convertToSearchPayloadMetadata(Class<?> resourceInformation);

    default MediaType getCreatePayloadMediaType(Class<?> resourceInformation) {
        return MediaType.APPLICATION_JSON;
    }
    default MediaType getUpdatePayloadMediaType(Class<?> resourceInformation) {
        return MediaType.APPLICATION_JSON;
    }
    default MediaType getSearchPayloadMediaType(Class<?> resourceInformation) {
        return MediaType.APPLICATION_JSON;
    }
}
