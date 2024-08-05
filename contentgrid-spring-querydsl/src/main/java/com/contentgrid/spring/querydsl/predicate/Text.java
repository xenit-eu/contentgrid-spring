package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public class Text {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractStringPredicateFactory extends AbstractSimpleQuerydslPredicateFactory<StringPath, String> {
        private final BiFunction<StringPath, String, BooleanExpression> stringExpressionMapper;

        @Override
        protected StringPath coercePath(Path<?> path) {
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
            if(values.size() == 1) {
                var value = values.iterator().next();
                return Optional.of(stringExpressionMapper.apply(path, value));
            }

            // If there are multiple values, return whether any of the provided values matches
            BooleanBuilder builder = new BooleanBuilder();
            values.forEach(value -> builder.or(stringExpressionMapper.apply(path, value)));

            return Optional.ofNullable(builder.getValue());
        }
    }

    /**
     * Filters items down to only items matching the supplied value in a case-insensitive way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class EqualsIgnoreCase extends AbstractStringPredicateFactory {

        public EqualsIgnoreCase() {
            super(StringExpression::equalsIgnoreCase);
        }
    }

    /**
     * Filters items down to only items starting with the supplied value.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWith extends AbstractStringPredicateFactory {

        public StartsWith() {
            super(StringExpression::startsWith);
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a case-insensitive way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithIgnoreCase extends AbstractStringPredicateFactory {

        public StartsWithIgnoreCase() {
            super(StringExpression::startsWithIgnoreCase);
        }
    }

    /**
     * Filters items down to only items matching the supplied value in a NFC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class EqualsNormalized extends AbstractStringPredicateFactory {

        public EqualsNormalized() {
            super((expr, value) -> normalize(expr).eq(Normalizer.normalize(value, Form.NFC)));
        }
    }

    /**
     * Filters items down to only items matching the supplied value in a case-insensitive, NFC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class EqualsIgnoreCaseNormalized extends AbstractStringPredicateFactory {

        public EqualsIgnoreCaseNormalized() {
            super((expr, value) -> normalize(expr).equalsIgnoreCase(Normalizer.normalize(value, Form.NFC)));
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a NFC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithIgnoreCaseNormalized extends AbstractStringPredicateFactory {

        protected StartsWithIgnoreCaseNormalized() {
            super((expr, value) -> normalize(expr).startsWithIgnoreCase(Normalizer.normalize(value, Form.NFC)));
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a case-insensitive, NFC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithNormalized extends AbstractStringPredicateFactory {

        protected StartsWithNormalized() {
            super((expr, value) -> normalize(expr).startsWith(Normalizer.normalize(value, Form.NFC)));
        }
    }

    static StringExpression normalize(StringExpression expr) {
        return Expressions.stringTemplate("normalize({0s})", expr);
    }

}
