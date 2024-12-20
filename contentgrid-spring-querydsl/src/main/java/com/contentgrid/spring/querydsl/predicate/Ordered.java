package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.NumberPath;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class Ordered {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractComparablePredicateFactory<T extends Comparable<T>> extends AbstractSimpleQuerydslPredicateFactory<Path<T>, T> {
        private final BiFunction<Stream<? extends T>, Comparator<T>, Optional<? extends T>> valueExtractor;
        private final Operation operation;

        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        enum Operation implements BiFunction<Path<?>, Object, Predicate> {
            LT(ComparableExpression::lt, castedFunction(NumberPath::lt), "less-than"),
            LTE(ComparableExpression::loe, castedFunction(NumberPath::loe), "less-than-or-equal"),
            GT(ComparableExpression::gt, castedFunction(NumberPath::gt), "greater-than"),
            GTE(ComparableExpression::goe, castedFunction(NumberPath::goe), "greater-than-or-equal");

            private final BiFunction<ComparableExpression<Comparable<?>>, Comparable<?>, Predicate> comparableBuilder;
            private final BiFunction<NumberPath<?>, Number, Predicate> numberBuilder;
            @Getter
            private final String name;

            @SuppressWarnings({"unchecked", "rawtypes"})
            private static <T extends Number & Comparable<T>, A extends Number&Comparable<?>> BiFunction<NumberPath<?>, Number, Predicate> castedFunction(BiFunction<NumberPath<T>, A, Predicate> fn) {
                return (BiFunction) fn;
            }

            @Override
            public Predicate apply(Path<?> comparableExpressionBase, Object o) {
                if(comparableExpressionBase instanceof ComparableExpression<?> p) {
                    return comparableBuilder.apply((ComparableExpression<Comparable<?>>) p, (Comparable) o);
                } else if(comparableExpressionBase instanceof NumberPath<?> p) {
                    return numberBuilder.apply(p, (Number)o);
                } else {
                    throw new UnsupportedOperationException("ComparableExpressionBase should be either ComparablePath or NumberPath");
                }
            }
        }


        @Override
        protected Path<T> coercePath(Path<?> path)
                throws UnsupportedCollectionFilterPredicatePathTypeException {
            if (path instanceof ComparableExpression<?> comparablePath) {
                return (Path<T>)comparablePath;
            } else if(path instanceof NumberPath<?> numberPath) {
                return (Path<T>)numberPath;
            }
            throw new UnsupportedCollectionFilterPredicatePathTypeException(this, path, Set.of(
                    ComparableExpression.class,
                    NumberPath.class
            ));
        }

        @Override
        protected Optional<Predicate> bindCoerced(Path<T> path, Collection<? extends T> values) {
            // If there are multiple values, typically they are AND'ed together, and we are only interested in the lowest or highest value.
            // x.lt=5&x.lt=2 -> x.lt=2
            var lowestValue = valueExtractor.apply(values.stream(), Comparable::compareTo);

            return lowestValue.map(value -> operation.apply(path, value));
        }

        @Override
        public String getFilterType() {
            return operation.getName();
        }
    }

    /**
     * Filters items down to only items strictly less than the supplied value.
     * <p>
     * This predicate only supports {@link Comparable<?>} types, and can not be used with any other types.
     */
    public static class LessThan<T extends Comparable<T>> extends AbstractComparablePredicateFactory<T> {
        public LessThan() {
            super(Stream::min, Operation.LT);
        }
    }

    /**
     * Filters items down to only items less than or equal to the supplied value.
     * <p>
     * This predicate only supports {@link Comparable<?>} types, and can not be used with any other types.
     */
    public static class LessThanOrEqual<T extends Comparable<T>> extends AbstractComparablePredicateFactory<T> {
        public LessThanOrEqual() {
            super(Stream::min, Operation.LTE);
        }
    }

    /**
     * Filters items down to only items strictly greater than the supplied value.
     * <p>
     * This predicate only supports {@link Comparable<?>} types, and can not be used with any other types.
     */
    public static class GreaterThan<T extends Comparable<T>> extends AbstractComparablePredicateFactory<T> {
        public GreaterThan() {
            super(Stream::max, Operation.GT);
        }
    }

    /**
     * Filters items down to only items greater than or equal to the supplied value.
     * <p>
     * This predicate only supports {@link Comparable<?>} types, and can not be used with any other types.
     */
    public static class GreaterThanOrEqual<T extends Comparable<T>> extends AbstractComparablePredicateFactory<T> {
        public GreaterThanOrEqual() {
            super(Stream::max, Operation.GTE);
        }
    }
}