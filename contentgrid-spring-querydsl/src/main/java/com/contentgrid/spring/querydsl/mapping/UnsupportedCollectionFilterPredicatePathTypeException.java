package com.contentgrid.spring.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class UnsupportedCollectionFilterPredicatePathTypeException extends UnsupportedCollectionFilterPredicateException {
    private final Set<Class<?>> expectedPathType;

    public UnsupportedCollectionFilterPredicatePathTypeException(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, Class<?> requiredPathType) {
        super(predicateFactory, path, "path is of type '%s', but should be of type '%s'".formatted(path.getClass(), requiredPathType));
        this.expectedPathType = Set.of(requiredPathType);
    }

    public UnsupportedCollectionFilterPredicatePathTypeException(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, Set<Class<?>> requiredPathType) {
        super(predicateFactory, path, "path is of type '%s', but should be any of %s".formatted(path.getClass(), requiredPathType.stream().map(Class::toString).collect(
                Collectors.joining("', '", "'", "'"))));
        this.expectedPathType = requiredPathType;
    }

}
