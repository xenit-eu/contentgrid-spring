package com.contentgrid.spring.querydsl.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BeanPath;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.TimePath;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.Jsr310Converters;

class CollectionFilterQuerydslPredicateConverterTest {

    private static CollectionFiltersMapping createMapping(CollectionFilter<?> filter) {
        return new CollectionFiltersMapping() {

            @Override
            public CollectionFilters forDomainType(Class<?> domainType) {
                return new CollectionFilters() {
                    @Override
                    public Stream<CollectionFilter<?>> filters() {
                        return Stream.of(filter);
                    }
                };
            }

            @Override
            public Optional<CollectionFilter<?>> forProperty(Class<?> domainType, String... properties) {
                return Optional.empty();
            }

            @Override
            public Optional<CollectionFilter<?>> forIdProperty(Class<?> domainType, String... properties) {
                return Optional.empty();
            }
        };
    }


    @Builder
    @Getter
    private static class TestCollectionFilter<T> implements CollectionFilter<T> {
        private final String filterName;
        @Builder.Default
        private final boolean documented = true;

        private final Path<T> path;

        private final Class<T> parameterType;

        Collection<T> lastParameters;

        @Override
        public Optional<Predicate> createPredicate(Collection<T> parameters) {
            lastParameters = parameters;
            return Optional.empty();
        }
    }

    @Value
    private static class TestObject {
        String stringValue;
        Instant timeValue;
        Boolean booleanValue;
        int intValue;
        UUID uuidValue;
    }


    private static class QTestObject extends BeanPath<TestObject> {
        public QTestObject(String variable) {
            super(TestObject.class, variable);
        }

        public QTestObject(Path<?> parent, String property) {
            super(TestObject.class, parent, property);
        }

        public QTestObject(PathMetadata metadata) {
            super(TestObject.class, metadata);
        }

        public QTestObject(PathMetadata metadata, PathInits inits) {
            super(TestObject.class, metadata, inits);
        }

        public final StringPath stringValue = createString("stringValue");
        public final TimePath<Instant> timeValue = createTime("timeValue", Instant.class);
        public final BooleanPath booleanValue = createBoolean("booleanValue");
        public final ComparablePath<UUID> uuidValue = createComparable("uuidValue", UUID.class);

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
                .build();
        var converter = new CollectionFilterQuerydslPredicateConverter(
                createMapping(filter),
                conversionService
        );

        converter.getPredicate(TestObject.class, Map.of("test", inputValues));

        assertThat(filter.lastParameters).isEqualTo(outputValues);
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
                .build();
        var converter = new CollectionFilterQuerydslPredicateConverter(
                createMapping(filter),
                conversionService
        );

        assertThatThrownBy(() -> converter.getPredicate(TestObject.class, Map.of("test", inputValues)))
                .isInstanceOfSatisfying(InvalidCollectionFilterValueException.class, exception -> {
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