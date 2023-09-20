package com.contentgrid.spring.querydsl.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.contentgrid.spring.querydsl.predicate.Ordered.GreaterThan;
import com.contentgrid.spring.querydsl.predicate.Ordered.GreaterThanOrEqual;
import com.contentgrid.spring.querydsl.predicate.Ordered.LessThan;
import com.contentgrid.spring.querydsl.predicate.Ordered.LessThanOrEqual;
import com.contentgrid.spring.querydsl.test.fixtures.QTestObject;
import com.contentgrid.spring.querydsl.test.predicate.PredicateFactoryTester;
import com.contentgrid.spring.querydsl.test.predicate.PredicateFactoryTester.CuriedPredicateFactory;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadataFactory;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OrderedTest {

    private final static PredicateFactoryTester<QTestObject> TESTER = new PredicateFactoryTester<>(new QTestObject(
            PathMetadataFactory.forVariable("o")));

    static Stream<Arguments> boundPredicates() {
        Instant early = Instant.parse("2021-10-10T07:05:09Z");
        Instant late = Instant.parse("2022-10-05T06:07:09Z");

        return Stream.of(
                // integers
                Arguments.of(TESTER.evaluate(new LessThan<Integer>(), QTestObject::intValue), List.of(123, 456), 123, "o.intValue < 123"),
                Arguments.of(TESTER.evaluate(new LessThanOrEqual<Integer>(), QTestObject::intValue), List.of(123, 456), 123, "o.intValue <= 123"),
                Arguments.of(TESTER.evaluate(new GreaterThan<Integer>(), QTestObject::intValue), List.of(123, 456), 456, "o.intValue > 456"),
                Arguments.of(TESTER.evaluate(new GreaterThanOrEqual<Integer>(), QTestObject::intValue), List.of(123, 456), 456, "o.intValue >= 456"),

                // strings
                Arguments.of(TESTER.evaluate(new LessThan<String>(), QTestObject::stringValue), List.of("a", "b"), "a", "o.stringValue < a"),
                Arguments.of(TESTER.evaluate(new LessThanOrEqual<String>(), QTestObject::stringValue), List.of("a", "b"), "a", "o.stringValue <= a"),
                Arguments.of(TESTER.evaluate(new GreaterThan<String>(), QTestObject::stringValue), List.of("a", "b"), "b", "o.stringValue > b"),
                Arguments.of(TESTER.evaluate(new GreaterThanOrEqual<String>(), QTestObject::stringValue), List.of("a", "b"), "b", "o.stringValue >= b"),

                // booleans
                Arguments.of(TESTER.evaluate(new LessThan<Boolean>(), QTestObject::booleanValue), List.of(false, true), false, "o.booleanValue < false"),
                Arguments.of(TESTER.evaluate(new LessThanOrEqual<Boolean>(), QTestObject::booleanValue), List.of(false, true), false, "o.booleanValue <= false"),
                Arguments.of(TESTER.evaluate(new GreaterThan<Boolean>(), QTestObject::booleanValue), List.of(false, true), true, "o.booleanValue > true"),
                Arguments.of(TESTER.evaluate(new GreaterThanOrEqual<Boolean>(), QTestObject::booleanValue), List.of(false, true), true, "o.booleanValue >= true"),

                // time
                Arguments.of(TESTER.evaluate(new LessThan<Instant>(), QTestObject::timeValue), List.of(early, late), early, "o.timeValue < "+early),
                Arguments.of(TESTER.evaluate(new LessThanOrEqual<Instant>(), QTestObject::timeValue), List.of(early, late), early, "o.timeValue <= "+early),
                Arguments.of(TESTER.evaluate(new GreaterThan<Instant>(), QTestObject::timeValue), List.of(early, late), late, "o.timeValue > "+late),
                Arguments.of(TESTER.evaluate(new GreaterThanOrEqual<Instant>(), QTestObject::timeValue), List.of(early, late), late, "o.timeValue >= "+late)
        );
    }

    static Stream<Class<? extends QuerydslPredicateFactory>> factories() {
        return Stream.of(
                LessThan.class,
                LessThanOrEqual.class,
                GreaterThan.class,
                GreaterThanOrEqual.class
        );
    }

    @ParameterizedTest
    @MethodSource("factories")
    void rejectsNonComparablePath(Class<QuerydslPredicateFactory<Path<?>, Integer>> type)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var factory = type.getDeclaredConstructor().newInstance();
        var tests = TESTER.evaluateAll(factory, Stream.of(
                QTestObject::embeddedObject,
                QTestObject::embeddedItems,
                QTestObject::stringItems
        ));

        assertThat(tests).allSatisfy(test -> {
            assertThatThrownBy(test::boundPaths)
                    .isInstanceOf(UnsupportedCollectionFilterPredicatePathTypeException.class);
            assertThatThrownBy(() -> test.bind(List.of(123)))
                    .isInstanceOf(UnsupportedCollectionFilterPredicatePathTypeException.class);
        });
    }

    @ParameterizedTest
    @MethodSource("boundPredicates")
    <T> void bindsPath(CuriedPredicateFactory<Path<?>, T> factory, List<T> items, T item, String stringValue) {
        assertThat(factory.boundPaths()).contains(factory.getPath());
        assertThat(factory.bind(List.of())).isEmpty();

        assertThat(factory.bind(List.of(item))).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString(stringValue);
        });

        assertThat(factory.bind(items)).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString(stringValue);
        });
    }
}
