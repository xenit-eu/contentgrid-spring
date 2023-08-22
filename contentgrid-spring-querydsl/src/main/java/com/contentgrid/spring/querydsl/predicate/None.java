package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Creates no predicate under any circumstances
 */
public class None implements QuerydslPredicateFactory<Path<?>, Object> {

    @Override
    public Stream<Path<?>> boundPaths(Path<?> path) {
        return Stream.empty();
    }

    @Override
    public Optional<Predicate> bind(Path<?> path, Collection<?> values) {
        return Optional.empty();
    }
}
