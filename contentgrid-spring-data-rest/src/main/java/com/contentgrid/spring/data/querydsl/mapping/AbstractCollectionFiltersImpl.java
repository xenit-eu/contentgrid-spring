package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Order;
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

    @Override
    public CollectionFilters forSorting() {
        return new PredicateCollectionFiltersImpl(
                this,
                cf -> !isCrossRelation(cf.getPath()) && cf.createOrderSpecifier(Order.ASC).isPresent()
        );
    }

    private static boolean isCrossRelation(Path<?> path) {
        while (path.getMetadata().getParent() != null) {
            if (path instanceof EntityPath<?>) {
                // This goes across a relation; we don't want to order across relations
                // Note that the root path always is an EntityPath, but it will never be reached due to the condition in the while-loop
                return true;
            }
            path = path.getMetadata().getParent();
        }
        return false;
    }
}
