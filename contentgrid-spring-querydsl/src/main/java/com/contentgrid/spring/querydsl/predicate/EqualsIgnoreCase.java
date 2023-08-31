package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;
import java.util.Collection;
import java.util.Optional;

/**
 * Compares strings in a case-insensitive way.
 *
 * This predicate only supports strings and can not be used with other types.
 */
public class EqualsIgnoreCase extends AbstractSimpleQuerydslPredicateFactory<StringPath, String> {

    @Override
    public StringPath coercePath(Path<?> path) {
        if(path instanceof StringPath stringPath) {
            return stringPath;
        }
        throw new UnsupportedCollectionFilterPredicatePathTypeException(this, path, StringPath.class);
    }

    @Override
    protected Optional<Predicate> bindCoerced(StringPath path, Collection<? extends String> values) {
        if(values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(path.lower().in(values.stream().map(String::toLowerCase).toList()));
    }
}
