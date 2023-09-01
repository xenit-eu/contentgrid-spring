package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;

public interface CollectionFilter<T> {
    String getFilterName();
    boolean isDocumented();
    Path<T> getPath();
    Class<T> getParameterType();
    Optional<Predicate> createPredicate(Collection<T> parameters);
}
