package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;

@FunctionalInterface
public interface PredicateFactoryInstantiator {
    QuerydslPredicateFactory<Path<?>, ?> instantiate(Class<? extends QuerydslPredicateFactory> clazz);
}
