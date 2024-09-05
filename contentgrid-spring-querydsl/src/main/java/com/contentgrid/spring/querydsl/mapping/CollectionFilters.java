package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Holds an ordered set of {@link CollectionFilter}s that can be further filtered down
 */
public interface CollectionFilters {

    /**
     * @return An ordered stream of filters present in this object
     */
    Stream<CollectionFilter<?>> filters();

    /**
     * Restricts filters to those that can be used for sorting results
     *
     * @return A reduced set of {@link CollectionFilter} that can be used for ordering results
     */
    default CollectionFilters forSorting() {
        return () -> CollectionFilters.this.filters()
                .filter(filter -> filter.createOrderSpecifier(Order.ASC).isPresent());
    }

    /**
     * Restrict filters to those that are documented
     * @return A reduced set of {@link CollectionFilter} that are documented
     */
    default CollectionFilters documented() {
        return () -> CollectionFilters.this.filters()
                .filter(CollectionFilter::isDocumented);
    }

    /**
     * Restricts filters to those that are applicable to a certain QueryDSL {@link Path}
     *
     * @param path The QueryDSL {@link Path} to restrict the filters to
     * @return A reduced set of {@link CollectionFilter} that can be used for a certain path
     */
    default CollectionFilters forPath(Path<?> path) {
        return () -> CollectionFilters.this.filters().filter(filter -> Objects.equals(filter.getPath(), path));
    }

    /**
     * Tries to locate a filter by its name
     *
     * @param filterName The filter name to locate the filter for
     */
    default Optional<CollectionFilter<?>> named(String filterName) {
        return filters().filter(filter -> Objects.equals(filter.getFilterName(), filterName)).findAny();
    }

}
