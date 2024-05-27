package com.contentgrid.spring.data.rest.webmvc;

import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.http.MediaType;

public interface DomainTypeToHalFormsPayloadMetadataConverter {
    PayloadMetadataAndMediaType convertToCreatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadataAndMediaType convertToUpdatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadata convertToSearchPayloadMetadata(Class<?> resourceInformation);

    record PayloadMetadataAndMediaType(PayloadMetadata payloadMetadata, MediaType mediaType){}
}
