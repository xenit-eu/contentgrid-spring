package com.contentgrid.spring.querydsl.annotations;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;

@FunctionalInterface
public interface QuerydslPredicateFactory<T extends Path<? extends S>, S> {
    Optional<Predicate> bind(T path, Collection<? extends S> values);
}
