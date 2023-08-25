package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PredicateCollectionFiltersImpl extends AbstractCollectionFiltersImpl{
    private final CollectionFilters collectionFilters;
    private final Predicate<CollectionFilter> predicate;

    @Override
    public Stream<CollectionFilter> filters() {
        return collectionFilters.filters().filter(predicate);
    }
}
