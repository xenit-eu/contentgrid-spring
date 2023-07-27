package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;

public class None implements QuerydslPredicateFactory<Path<?>, Object> {

    @Override
    public Optional<Predicate> bind(Path<?> path, Collection<?> values) {
        return Optional.empty();
    }
}
