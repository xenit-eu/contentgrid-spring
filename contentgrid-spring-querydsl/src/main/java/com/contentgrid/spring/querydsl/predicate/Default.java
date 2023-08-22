package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BeanPath;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Default implementation that creates a {@link Predicate} based on the {@link Path}s type.
 */
public class Default implements QuerydslPredicateFactory<Path<?>, Object> {

    @Override
    public Stream<Path<?>> boundPaths(Path<?> path) {
        if(shouldHandlePath(path)) {
            return Stream.of(path);
        } else {
            return Stream.empty();
        }
    }

    private boolean shouldHandlePath(Path<?> path) {
        if(path instanceof BeanPath<?>) {
            // A BeanPath means that the path is to an object that has properties.
            // These can not be handled directly by this PredicateFactory, as performing equal/in on an object does not work
            return false;
        }
        if(path instanceof SimpleExpression<?>) {

            return true;
        }
        if(path instanceof CollectionPathBase<?, ?, ?> collectionPathBase) {
            // If there is a collection, check if we handle the collection value.
            // Like the top level, nested beanpaths will not be handled, but other simple expressions will be handled
            return shouldHandlePath((Path<?>) collectionPathBase.any());
        }

        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<Predicate> bind(@NonNull Path<? extends Object> path, @NonNull Collection<? extends Object> values) {
        if(values.isEmpty()) {
            return Optional.empty();
        }

        if(path instanceof CollectionPathBase collectionPath) {
            BooleanBuilder builder = new BooleanBuilder();

            for (Object value : values) {
                builder.and(collectionPath.contains(value));
            }

            return Optional.of(builder.getValue());
        }

        if(path instanceof SimpleExpression expression) {
            if(values.size() > 1) {
                return Optional.of(expression.in(values));
            }

            Object item = values.iterator().next();

            if(item == null) {
                return Optional.of(expression.isNull());
            } else {
                return Optional.of(expression.eq(item));
            }
        }

        throw new IllegalArgumentException("Can not create predicate for path '%s'".formatted(path));
    }
}
