package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Compares strings in a case-insensitive way.
 *
 * This predicate only supports strings and can not be used with other types.
 */
public class EqualsIgnoreCase implements QuerydslPredicateFactory<StringPath, String> {

    @Override
    public Stream<StringPath> boundPaths(Path<?> path) {
        if(path instanceof StringPath stringPath) {
            return Stream.of(stringPath);
        }
        throw new UnsupportedCollectionFilterPredicatePathTypeException(this, path, StringPath.class);
    }

    @Override
    public Optional<Predicate> bind(StringPath path, Collection<? extends String> values) {
        if(values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(path.lower().in(values.stream().map(String::toLowerCase).toList()));

    }
}
