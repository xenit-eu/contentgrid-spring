package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;

public interface CollectionFilter<T> {
    String getFilterName();
    boolean isDocumented();
    Path<T> getPath();
    Class<T> getParameterType();
    Optional<Predicate> createPredicate(Collection<T> parameters);

    /**
     * Creates a sort order specification from the <code>sort</code> request query parameter
     * <p>
     * Not all types of collection filter support sorting. If sorting is not supported, an empty optional will be
     * returned.
     *
     * @param order The direction the property should be sorted in
     * @return An order specification if one can be created for the specified order and collection filter
     */
    Optional<OrderSpecifier<?>> createOrderSpecifier(Order order);
}
