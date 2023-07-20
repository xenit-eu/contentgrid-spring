package com.contentgrid.spring.data.rest.webmvc;

import org.springframework.hateoas.AffordanceModel.PayloadMetadata;

public interface DomainTypeToHalFormsPayloadMetadataConverter {
    PayloadMetadata convertToCreatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadata convertToUpdatePayloadMetadata(Class<?> resourceInformation);
    PayloadMetadata convertToSearchPayloadMetadata(Class<?> resourceInformation);
}
