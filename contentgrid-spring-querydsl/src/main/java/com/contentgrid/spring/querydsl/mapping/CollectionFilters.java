package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Path;
import java.util.stream.Stream;

public interface CollectionFilters {
    Stream<CollectionFilter> filters();

    CollectionFilters forPath(Path<?> path);

}
