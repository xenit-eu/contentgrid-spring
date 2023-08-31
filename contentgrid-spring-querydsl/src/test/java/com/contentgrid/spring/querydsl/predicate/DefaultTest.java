package com.contentgrid.spring.querydsl.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicateException;
import com.contentgrid.spring.querydsl.test.fixtures.QTestObject;
import com.contentgrid.spring.querydsl.test.predicate.PredicateFactoryTester;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadataFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.Jsr310Converters;

class DefaultTest {
    private final Default DEFAULT_FACTORY = new Default();

    private final PredicateFactoryTester<QTestObject> TESTER = new PredicateFactoryTester<>(new QTestObject(PathMetadataFactory.forVariable("o")));

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();
    static {
        Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class TypeValueArgument<Q, T> implements Arguments {
        private final Function<Q, Path<? extends T>> path;
        private final T value;

        @Override
        public Object[] get() {
            return new Object[] {path, value};
        }
    }

    public static Stream<Arguments> simpleTypes() {
        return Stream.of(
                TypeValueArgument.of(QTestObject::stringValue, "abc"),
                TypeValueArgument.of(QTestObject::booleanValue, true),
                TypeValueArgument.of(QTestObject::timeValue, Instant.now()),
                TypeValueArgument.of(QTestObject::uuidValue, UUID.randomUUID())
        );
    }

    @ParameterizedTest
    @MethodSource
    void simpleTypes(Function<QTestObject, Path<?>> path, Object value) {
        var factory = TESTER.evaluate(DEFAULT_FACTORY, path);

        assertThat(factory.boundPaths()).hasSize(1);

        var p = factory.boundPaths().findFirst().get();
        assertThat(factory.bind(List.of())).isEmpty();
        assertThat(factory.bind(List.of(value))).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString(p+" = "+value);
        });
        assertThat(factory.bind(List.of(value, value))).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString(p+" in ["+value+ ", "+value+"]");
        });
    }

    @Test
    void collectionType() {
        var factory = TESTER.evaluate(DEFAULT_FACTORY, QTestObject::stringItems);
        assertThat(factory.boundPaths()).hasSize(1);

        assertThat(factory.bind(List.of())).isEmpty();
        assertThat(factory.bind(List.of("abc"))).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString("abc in o.stringItems");
        });
        assertThat(factory.bind(List.of("abc", "def"))).hasValueSatisfying(predicate -> {
            assertThat(predicate).hasToString("abc in o.stringItems && def in o.stringItems");
        });
    }

    @Test
    void embeddedType() {
        var factory = TESTER.evaluate(DEFAULT_FACTORY, QTestObject::embeddedObject);

        assertThat(factory.boundPaths()).isEmpty();

        assertThatThrownBy(() -> factory.bind(List.of("abc")))
                .isInstanceOf(UnsupportedCollectionFilterPredicateException.class);
    }

    @Test
    void embeddedCollectionType() {
        var factory = TESTER.evaluate(DEFAULT_FACTORY, QTestObject::embeddedItems);

        assertThat(factory.boundPaths()).isEmpty();

        assertThatThrownBy(() -> factory.bind(List.of("abc")))
                .isInstanceOf(UnsupportedCollectionFilterPredicateException.class);
    }

}