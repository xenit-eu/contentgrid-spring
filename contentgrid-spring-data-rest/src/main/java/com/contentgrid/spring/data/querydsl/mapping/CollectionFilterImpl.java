package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CollectionFilterImpl<T> implements CollectionFilter<T> {

    @Getter
    private final String filterName;

    @Getter
    private final Path<T> path;

    @Getter
    private final boolean documented;

    private final Path<?> originalPath;

    private final QuerydslPredicateFactory<Path<?>, Object> predicateFactory;

    @Override
    public AnnotatedElement getAnnotatedElement() {
        return originalPath.getAnnotatedElement();
    }

    @Override
    public Class<T> getParameterType() {
        return (Class<T>) predicateFactory.valueType(originalPath);
    }

    @Override
    public Optional<Predicate> createPredicate(Collection<T> parameters) {
        return predicateFactory.bind(originalPath, parameters);
    }

    @Override
    public Optional<OrderSpecifier<?>> createOrderSpecifier(Order order) {
        return predicateFactory.sortExpression(originalPath)
                .map(expr -> new OrderSpecifier<>(order, expr));
    }
}
