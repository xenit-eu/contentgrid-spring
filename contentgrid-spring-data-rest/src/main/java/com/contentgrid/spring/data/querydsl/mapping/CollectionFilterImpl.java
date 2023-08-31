package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.converter.InvalidCollectionFilterValueException;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

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
    public Class<T> getParameterType() {
        return (Class<T>) predicateFactory.valueType(originalPath);
    }

    @Override
    public Optional<Predicate> createPredicate(Collection<T> parameters) {
        return predicateFactory.bind(originalPath, parameters);
    }
}
