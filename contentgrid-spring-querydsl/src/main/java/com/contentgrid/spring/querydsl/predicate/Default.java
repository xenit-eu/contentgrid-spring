package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;
import java.util.Collection;
import java.util.Optional;
import lombok.NonNull;

/**
 * Default implementation that creates a {@link Predicate} based on the {@link Path}s type.
 */
public class Default implements QuerydslPredicateFactory<Path<?>, Object> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<Predicate> bind(@NonNull Path<?> path, @NonNull Collection<? extends Object> values) {
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
