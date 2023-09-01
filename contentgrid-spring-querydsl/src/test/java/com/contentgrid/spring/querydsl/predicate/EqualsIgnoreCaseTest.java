package com.contentgrid.spring.querydsl.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.contentgrid.spring.querydsl.test.fixtures.QTestObject;
import com.contentgrid.spring.querydsl.test.predicate.PredicateFactoryTester;
import com.querydsl.core.types.PathMetadataFactory;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EqualsIgnoreCaseTest {
    private final EqualsIgnoreCase FACTORY = new EqualsIgnoreCase();

    private final PredicateFactoryTester<QTestObject> TESTER = new PredicateFactoryTester<>(new QTestObject(
            PathMetadataFactory.forVariable("o")));

    @Test
    void rejectsNonStringPath() {
        var tests = TESTER.evaluateAll(FACTORY, Stream.of(
                QTestObject::booleanValue,
                QTestObject::embeddedObject,
                QTestObject::embeddedItems,
                QTestObject::timeValue,
                QTestObject::uuidValue
        ));

        assertThat(tests).allSatisfy(test -> {
            assertThatThrownBy(test::boundPaths)
                    .isInstanceOf(UnsupportedCollectionFilterPredicatePathTypeException.class);
            assertThatThrownBy(() -> test.bind(List.of("abc")))
                    .isInstanceOf(UnsupportedCollectionFilterPredicatePathTypeException.class);
        });
    }

    @Test
    void bindsStringPath() {
        var factory = TESTER.evaluate(FACTORY, QTestObject::stringValue);

        assertThat(factory.boundPaths()).containsExactly(TESTER.getPathBase().stringValue);
        assertThat(factory.bind(List.of())).isEmpty();
        assertThat(factory.bind(List.of("abc"))).hasValueSatisfying(predicate -> {
            assertThat(predicate).isEqualTo(TESTER.getPathBase().stringValue.equalsIgnoreCase("abc"));
        });
        assertThat(factory.bind(List.of("ABCdef"))).hasValueSatisfying(predicate -> {
            assertThat(predicate).isEqualTo(TESTER.getPathBase().stringValue.equalsIgnoreCase("ABCdef"));
        });
        assertThat(factory.bind(List.of("ABCdef", "GHI"))).hasValueSatisfying(predicate -> {
            assertThat(predicate).isEqualTo(TESTER.getPathBase().stringValue.lower().in("abcdef", "ghi"));
        });
    }

}