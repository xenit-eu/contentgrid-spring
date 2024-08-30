package com.contentgrid.spring.querydsl.annotation;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Factory for QueryDSL {@link Predicate}s from a {@link CollectionFilterParam}
 *
 * @param <T> Type of the path that the factory can be used for
 * @param <S> Type of the values in collection that will be mapped to the path
 */
public interface QuerydslPredicateFactory<T extends Path<?>,  S> {

    /**
     * Lists paths that are being bound to by this factory
     * <p>
     * This method is used to set up a reverse mapping from {@link Path}s to the {@link com.contentgrid.spring.querydsl.mapping.CollectionFilter} that binds them.
     *
     * @param path Property path at the position of the {@link CollectionFilterParam} annotation that references this factory
     * @return All paths that are being bound to by {@link #bind(Path, Collection)}
     */
    Stream<Path<?>> boundPaths(T path);

    /**
     * Create a predicate by binding the path to query parameter values
     *
     * @param path Property path at the position of the {@link CollectionFilterParam} annotation that references this factory
     * @param values Values for the query parameter bound by {@link CollectionFilterParam}, converted to the type requested by {@link #valueType(Path)}
     * @return A {@link Predicate} that does something with the path and values
     */
    Optional<Predicate> bind(T path, Collection<? extends S> values);

    /**
     * Specifies the type to coerce query parameter collection values to
     *
     * @param path Property path at the position of the {@link CollectionFilterParam} annotation that references this factory
     * @return Type to coerce query parameter collection values to
     */
    Class<? extends S> valueType(T path);

    /**
     * Create expressions to sort by this {@link CollectionFilterParam}
     *
     * @param path Property path at the position of the {@link CollectionFilterParam} annotation that references this
     * factory
     * @return All {@link Expression}s that will be used for sorting on this field
     */
    default Optional<Expression<? extends Comparable<?>>> sortExpression(T path) {
        return Optional.empty();
    }
}
