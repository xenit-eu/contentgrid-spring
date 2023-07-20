package com.contentgrid.spring.data.rest.webmvc;

import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;

public interface RootResourceInformationToHalFormsPayloadMetadataConverter {
    PayloadMetadata convertToCreatePayloadMetadata(RootResourceInformation resourceInformation);
    PayloadMetadata convertToUpdatePayloadMetadata(RootResourceInformation resourceInformation);
}
