package com.contentgrid.spring.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import lombok.Getter;

@Getter
public class UnsupportedCollectionFilterPredicateException extends UnsupportedOperationException{
    private final Path<?> path;

    public UnsupportedCollectionFilterPredicateException(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, String message) {
        super(createMessage(predicateFactory, path, message));
        this.path = path;
    }

    private static String createMessage(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, String message) {
        return "Predicate '%s' can not be used for path %s: %s".formatted(predicateFactory, path, message);
    }
}
