package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.querydsl.core.types.Path;
import java.util.Objects;

abstract class AbstractCollectionFiltersImpl implements CollectionFilters {
    @Override
    public CollectionFilters forPath(Path<?> path) {
        return new PredicateCollectionFiltersImpl(
                this,
                cf -> Objects.equals(cf.getPath(), path)
        );
    }

}
