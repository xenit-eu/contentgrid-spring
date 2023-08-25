package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CollectionFilterImpl implements CollectionFilter {
    @Getter
    private final String filterName;

    @Getter
    private final Path<? extends Object> path;

    @Getter
    private final boolean documented;

    private final QuerydslPredicateFactory<Path<?>, Object> predicateFactory;

    @Override
    public Optional<Predicate> createPredicate(Collection<?> parameters) {
        return predicateFactory.bind(path, parameters);
    }
}
