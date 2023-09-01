package com.contentgrid.spring.querydsl.test.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public class TestCollectionFilters implements CollectionFilters {
    final Collection<CollectionFilter<?>> filters = new ArrayList<>();

    @Override
    public Stream<CollectionFilter<?>> filters() {
        return filters.stream();
    }
}
