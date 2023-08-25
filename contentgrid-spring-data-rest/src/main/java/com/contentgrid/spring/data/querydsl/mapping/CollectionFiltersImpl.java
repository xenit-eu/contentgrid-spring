package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CollectionFiltersImpl extends AbstractCollectionFiltersImpl {
    private final Map<String, CollectionFilter> filters;

    @Override
    public Stream<CollectionFilter> filters() {
        return filters.values().stream();
    }

}
