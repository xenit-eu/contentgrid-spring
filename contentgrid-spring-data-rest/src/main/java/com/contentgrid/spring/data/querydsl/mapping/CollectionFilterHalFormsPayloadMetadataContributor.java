package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.data.rest.hal.forms.BasicPropertyMetadata;
import com.contentgrid.spring.data.rest.webmvc.HalFormsPayloadMetadataContributor;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;

@RequiredArgsConstructor
public class CollectionFilterHalFormsPayloadMetadataContributor implements HalFormsPayloadMetadataContributor {

    private final CollectionFiltersMapping collectionFiltersMapping;

    @Override
    public Stream<PropertyMetadata> contributeToCreateForm(Class<?> domainType) {
        return Stream.empty();
    }

    @Override
    public Stream<PropertyMetadata> contributeToUpdateForm(Class<?> domainType) {
        return Stream.empty();
    }

    @Override
    public Stream<PropertyMetadata> contributeToSearchForm(Class<?> domainType) {
        return collectionFiltersMapping.forDomainType(domainType)
                .documented()
                .filters()
                .map(filter -> new BasicPropertyMetadata(
                                filter.getFilterName(),
                                ResolvableType.forClass(filter.getParameterType())
                        )
                                .withReadOnly(false)
                );
    }
}
