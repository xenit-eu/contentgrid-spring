package com.contentgrid.spring.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import lombok.Getter;

@Getter
public class UnsupportedCollectionFilterPredicatePathTypeException extends UnsupportedCollectionFilterPredicateException {
    private final Class<?> expectedPathType;

    public UnsupportedCollectionFilterPredicatePathTypeException(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, Class<?> requiredPathType) {
        super(predicateFactory, path, "path is of type '%s', but should be of type '%s'".formatted(path.getClass(), requiredPathType));
        this.expectedPathType = requiredPathType;
    }

}
