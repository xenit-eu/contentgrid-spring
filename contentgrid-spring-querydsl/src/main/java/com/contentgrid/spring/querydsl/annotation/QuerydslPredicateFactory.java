package com.contentgrid.spring.querydsl.annotation;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface QuerydslPredicateFactory<T extends Path<? extends S>, S> {

    default Stream<Path<?>> boundPaths(T path) {
        return Stream.of(path);
    }

    Optional<Predicate> bind(T path, Collection<? extends S> values);
}
