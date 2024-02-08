package com.contentgrid.spring.querydsl.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.querydsl.test.fixtures.QTestObject;
import com.contentgrid.spring.querydsl.test.fixtures.TestObject;
import com.contentgrid.spring.querydsl.test.mapping.TestCollectionFilter;
import com.contentgrid.spring.querydsl.test.mapping.TestCollectionFiltersMapping;
import com.querydsl.core.types.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.Jsr310Converters;

class CollectionFilterQuerydslPredicateConverterTest {

    private static CollectionFiltersMapping createMapping(CollectionFilter<?> filter) {
        return new TestCollectionFiltersMapping()
                .addFilter(TestObject.class, filter);
    }


    private static final QTestObject TEST_PATH = new QTestObject("o");

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    static {
        Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);
    }

    @ParameterizedTest
    @MethodSource
    <T> void typeConversion(Path<T> path, Collection<String> inputValues, Collection<T> outputValues) {
        var filter = TestCollectionFilter.<T>builder()
                .filterName("test")
                .path(path)
                .parameterType((Class<T>)path.getType())
                .build();
        var converter = new CollectionFilterQuerydslPredicateConverter(
                createMapping(filter),
                conversionService
        );

        converter.getPredicate(TestObject.class, Map.of("test", inputValues));

        assertThat(filter.getLastParameters()).isEqualTo(outputValues);
    }

    public static Stream<Arguments> typeConversion() {
        return Stream.of(
                Arguments.of(TEST_PATH.stringValue, List.of("abc"), List.of("abc")),
                Arguments.of(TEST_PATH.booleanValue, List.of("true"), List.of(true)),
                Arguments.of(TEST_PATH.timeValue, List.of("1994-08-10T22:15:00Z"), List.of(Instant.parse("1994-08-10T22:15:00Z"))),
                Arguments.of(TEST_PATH.uuidValue, List.of("a1a8f7a8-4283-11ee-852b-8353804234d2"), List.of(UUID.fromString("a1a8f7a8-4283-11ee-852b-8353804234d2")))
        );
    }

    @ParameterizedTest
    @MethodSource
    <T> void failedTypeConversion(Path<T> path, Collection<String> inputValues) {
        var filter = TestCollectionFilter.<T>builder()
                .filterName("test")
                .path(path)
                .parameterType((Class<T>)path.getType())
                .build();
        var converter = new CollectionFilterQuerydslPredicateConverter(
                createMapping(filter),
                conversionService
        );

        assertThatThrownBy(() -> converter.getPredicate(TestObject.class, Map.of("test", inputValues)))
                .isInstanceOfSatisfying(CollectionFilterValueConversionException.class, exception -> {
                    assertThat(exception.getFilter()).isEqualTo(filter);
                    assertThat(exception.getInvalidValue()).isEqualTo(inputValues.iterator().next());
                });
    }

    public static Stream<Arguments> failedTypeConversion() {
        return Stream.of(
                Arguments.of(TEST_PATH.booleanValue, List.of("ZZZ")),
                Arguments.of(TEST_PATH.uuidValue, List.of("123")),
                Arguments.of(TEST_PATH.timeValue, List.of("2022-01-05"))
        );
    }

}