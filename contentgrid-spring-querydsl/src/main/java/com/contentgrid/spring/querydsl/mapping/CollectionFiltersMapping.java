package com.contentgrid.spring.querydsl.mapping;

import java.util.Optional;

public interface CollectionFiltersMapping {
    CollectionFilters forDomainType(Class<?> domainType);

    Optional<CollectionFilter<?>> forProperty(Class<?> domainType, String ...properties);

    Optional<CollectionFilter<?>> forIdProperty(Class<?> domainType, String... properties);
}
