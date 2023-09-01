package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

/**
 * Base class for "simple" {@link QuerydslPredicateFactory}s.
 * <p>
 * A simple predicate factory binds exactly one path the {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam} annotation is placed on
 *
 * @param <T> The necessary type of path this factory can bind to
 * @param <S> Type of the values in collection that will be mapped to the path
 */
@RequiredArgsConstructor
public abstract class AbstractSimpleQuerydslPredicateFactory<T extends Path<S>, S> implements QuerydslPredicateFactory<Path<?>, S> {
    @Override
    final public Stream<Path<?>> boundPaths(Path<?> path) {
        return Stream.of(coercePath(path));
    }

    @Override
    final public Optional<Predicate> bind(Path<?> path, Collection<? extends S> values) {
        return bindCoerced(coercePath(path), values);
    }

    /**
     * Converts the generic path type to a specific type that {@link #bindCoerced(Path, Collection)} can work upon
     *
     * @param path Property path at the position of the {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam} annotation that references this factory
     * @return The concrete path that this factory is bound to
     * @throws UnsupportedCollectionFilterPredicatePathTypeException When the passed path is not of the correct type
     */
    protected abstract T coercePath(Path<?> path) throws UnsupportedCollectionFilterPredicatePathTypeException;

    @Override
    public Class<? extends S> valueType(Path<?> path) {
        return coercePath(path).getType();
    }

    /**
     * Specialization of {@link #bind(Path, Collection)} with a pre-coerced {@link Path}
     *
     * @param path Path created by {@link #coercePath(Path)}
     * @param values Values for the query parameter bound by {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam}
     * @return A {@link Predicate} that does something with the path and values
     */
    protected abstract Optional<Predicate> bindCoerced(T path, Collection<? extends S> values);
}
