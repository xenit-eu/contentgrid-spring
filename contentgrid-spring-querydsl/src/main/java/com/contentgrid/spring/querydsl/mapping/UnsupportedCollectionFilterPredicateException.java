package com.contentgrid.spring.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import lombok.Getter;


/**
 * Exception thrown when due to <i>programmer error</i>, a predicate is used that does not support the path it's bound
 * to
 * <p>
 * This exception, and it's subclasses always indicate programmer error. Carefully check the selected
 * {@link CollectionFilterParam#predicate()} implementation to check if it supports the annotated property
 */
@Getter
public class UnsupportedCollectionFilterPredicateException extends UnsupportedOperationException {

    private final Path<?> path;

    public UnsupportedCollectionFilterPredicateException(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path,
            String message) {
        super(createMessage(predicateFactory, path, message));
        this.path = path;
    }

    private static String createMessage(QuerydslPredicateFactory<?, ?> predicateFactory, Path<?> path, String message) {
        return "Predicate '%s' can not be used for path %s: %s".formatted(predicateFactory, path, message);
    }
}
