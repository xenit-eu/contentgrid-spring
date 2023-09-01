package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public interface CollectionFilters {
    Stream<CollectionFilter<?>> filters();

    default CollectionFilters forPath(Path<?> path) {
        return () -> CollectionFilters.this.filters().filter(filter -> Objects.equals(filter.getPath(), path));
    }

    default Optional<CollectionFilter<?>> named(String filterName) {
        return filters().filter(filter -> Objects.equals(filter.getFilterName(), filterName)).findAny();
    };

}
