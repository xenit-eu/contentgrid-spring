package com.contentgrid.spring.querydsl.mapping;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Optional;

public interface CollectionFilter {
    String getFilterName();
    Path<?> getPath();
    Optional<Predicate> createPredicate(Collection<?> parameters);
}
