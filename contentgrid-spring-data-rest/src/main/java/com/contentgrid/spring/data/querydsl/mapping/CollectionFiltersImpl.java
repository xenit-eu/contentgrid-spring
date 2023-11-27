package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CollectionFiltersImpl extends AbstractCollectionFiltersImpl {
    private final Map<String, CollectionFilter<?>> filters;

    @Override
    public Stream<CollectionFilter<?>> filters() {
        return filters.values().stream();
    }

    @Override
    public Optional<CollectionFilter<?>> named(String filterName) {
        return Optional.ofNullable(filters.get(filterName));
    }

}
