package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Text {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private abstract static class AbstractStringPredicateFactory extends AbstractSimpleQuerydslPredicateFactory<StringPath, String> {
        private final BiFunction<StringPath, String, BooleanExpression> stringExpressionMapper;

        @Getter
        private final String filterType;

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
            super(StringExpression::equalsIgnoreCase, "case-insensitive-match");
        }

        @Override
        protected Optional<Predicate> bindCoerced(StringPath path, Collection<? extends String> values) {
            if (values.size() <= 1) {
                return super.bindCoerced(path, values);
            }
            return Optional.of(path.lower().in(values.stream().map(String::toLowerCase).toList()));
        }

        @Override
        public Optional<Expression<? extends Comparable<?>>> sortExpression(Path<?> path) {
            return Optional.of(coercePath(path).lower());
        }
    }

    /**
     * Filters items down to only items starting with the supplied value.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWith extends AbstractStringPredicateFactory {

        public StartsWith() {
            super(StringExpression::startsWith, "starts-with");
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a case-insensitive way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithIgnoreCase extends AbstractStringPredicateFactory {

        public StartsWithIgnoreCase() {
            super(StringExpression::startsWithIgnoreCase, "case-insensitive-starts-with");
        }
    }

    /**
     * Filters items down to only items matching the supplied value in a NFKC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class EqualsNormalized extends AbstractStringPredicateFactory {

        public EqualsNormalized() {
            super((expr, value) -> postgresNormalize(expr).eq(Normalizer.normalize(value, Form.NFKC)),
                    "exact-match");
        }

        @Override
        protected Optional<Predicate> bindCoerced(StringPath path, Collection<? extends String> values) {
            if (values.size() <= 1) {
                return super.bindCoerced(path, values);
            }

            return Optional.of(postgresNormalize(path).in(values.stream()
                    .map(value -> Normalizer.normalize(value, Form.NFKC))
                    .toList()));
        }

        @Override
        public Optional<Expression<? extends Comparable<?>>> sortExpression(Path<?> path) {
            return Optional.of(postgresNormalize(coercePath(path)));
        }
    }

    /**
     * Filters items down to only items matching the supplied value in a case-insensitive, NFKC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class EqualsIgnoreCaseNormalized extends AbstractStringPredicateFactory {

        public EqualsIgnoreCaseNormalized() {
            super((expr, value) -> postgresNormalize(expr).equalsIgnoreCase(Normalizer.normalize(value, Form.NFKC)),
                    "case-insensitive-match");
        }

        @Override
        protected Optional<Predicate> bindCoerced(StringPath path, Collection<? extends String> values) {
            if (values.size() <= 1) {
                return super.bindCoerced(path, values);
            }

            return Optional.of(postgresNormalize(path).lower().in(values.stream()
                    .map(value -> Normalizer.normalize(value, Form.NFKC).toLowerCase())
                    .toList()));
        }
        @Override
        public Optional<Expression<? extends Comparable<?>>> sortExpression(Path<?> path) {
            return Optional.of(postgresNormalize(coercePath(path)).lower());
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a NFKC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithNormalized extends AbstractStringPredicateFactory {

        protected StartsWithNormalized() {
            super((expr, value) -> postgresNormalize(expr).startsWith(Normalizer.normalize(value, Form.NFKC)),
                    "starts-with");
        }
    }

    /**
     * Filters items down to only items starting with the supplied value in a case-insensitive, NFKC normalized way.
     * <p>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class StartsWithIgnoreCaseNormalized extends AbstractStringPredicateFactory {

        protected StartsWithIgnoreCaseNormalized() {
            super((expr, value) -> postgresNormalize(expr).startsWithIgnoreCase(Normalizer.normalize(value, Form.NFKC)),
                    "case-insensitive-starts-with");
        }
    }

    /**
     * Filters items down to only items starting with the supplied value
     * in a case-insensitive, accent-insensitive, NFKC normalized way.
     * <p>
     * Requires Postgres extension {@code unaccent} and a function named
     * {@code contentgrid_prefix_search_normalize} defined in a schema named {@code extensions}:
     * <pre>
     * CREATE SCHEMA extensions;
     * CREATE EXTENSION unaccent SCHEMA extensions;
     * CREATE OR REPLACE FUNCTION extensions.contentgrid_prefix_search_normalize(arg text)
     *   RETURNS text
     *   LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
     * RETURN extensions.unaccent('extensions.unaccent', lower(normalize(arg, NFKC)));
     * </pre>
     * This predicate only supports {@link String}s, and can not be used with other types.
     */
    public static class ContentGridPrefixSearch extends AbstractStringPredicateFactory {

        protected ContentGridPrefixSearch() {
            // Using like() and manually append '%' to inner expression because startsWith() appends '%' to outer expression
            super((expr, value) -> contentGridPrefixSearchNormalize(expr)
                    .like(contentGridPrefixSearchNormalizePattern(ConstantImpl.create(value), "{0%}")),
                    "prefix-match");
        }
    }

    static StringExpression postgresNormalize(Expression<String> expr) {
        return Expressions.stringTemplate("normalize({0s})", expr);
    }

    static StringExpression contentGridPrefixSearchNormalize(Expression<String> expr) {
        return contentGridPrefixSearchNormalizePattern(expr, "{0s}");
    }

    static StringExpression contentGridPrefixSearchNormalizePattern(Expression<String> expr, String pattern) {
        return Expressions.stringTemplate("contentgrid_prefix_search_normalize(%s)".formatted(pattern), expr);
    }

}
