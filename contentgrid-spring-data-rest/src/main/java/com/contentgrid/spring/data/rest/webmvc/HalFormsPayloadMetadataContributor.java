package com.contentgrid.spring.data.rest.webmvc;

import java.util.stream.Stream;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;

/**
 * Contributes properties to HAL-FORMS templates based on their domain type
 */
public interface HalFormsPayloadMetadataContributor {

    Stream<PropertyMetadata> contributeToCreateForm(Class<?> domainType);

    Stream<PropertyMetadata> contributeToUpdateForm(Class<?> domainType);

    Stream<PropertyMetadata> contributeToSearchForm(Class<?> domainType);
}
