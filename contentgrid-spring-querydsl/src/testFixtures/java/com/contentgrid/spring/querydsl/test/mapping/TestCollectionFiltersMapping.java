package com.contentgrid.spring.querydsl.test.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestCollectionFiltersMapping implements CollectionFiltersMapping {
    private final Map<Class<?>, TestCollectionFilters> mapping = new HashMap<>();

    public TestCollectionFiltersMapping addFilter(Class<?> domainType, CollectionFilter<?> filter) {
        mapping.computeIfAbsent(domainType, _key -> new TestCollectionFilters())
                .filters
                .add(filter);
        return this;
    }

    @Override
    public CollectionFilters forDomainType(Class<?> domainType) {
        return mapping.get(domainType);
    }

    @Override
    public Optional<CollectionFilter<?>> forProperty(Class<?> domainType, String... properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CollectionFilter<?>> forIdProperty(Class<?> domainType, String... properties) {
        throw new UnsupportedOperationException();
    }
}
