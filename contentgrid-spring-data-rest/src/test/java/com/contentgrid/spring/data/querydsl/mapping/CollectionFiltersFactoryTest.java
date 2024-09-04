package com.contentgrid.spring.data.querydsl.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.data.rest.mapping.typeinfo.TypeInformationContainer;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

class CollectionFiltersFactoryTest {

    private static CollectionFiltersFactory createFactoryFor(EntityPathBase<?> path) {
        return new CollectionFiltersFactory(
                new DirectPredicateFactoryInstantiator(),
                "",
                new TypeInformationContainer(TypeInformation.of(path.getType())),
                new PathNavigator(path)
        );
    }

    @Entity
    public static class TestEntityWithDuplicateNames {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID myId;

        @CollectionFilterParam
        private String field1;

        @CollectionFilterParam("field1")
        private String field2;

    }

    @Entity
    public static class TestEntityWithMultipleBoundPredicate {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID myId;

        @CollectionFilterParam(predicate = MultipleBoundPredicate.class)
        private String field1;

    }

    private static class MultipleBoundPredicate implements QuerydslPredicateFactory<StringPath, String> {

        @Override
        public Stream<Path<?>> boundPaths(StringPath path) {
            return Stream.of(
                    path,
                    path.getMetadata().getParent()
            );
        }

        @Override
        public Optional<Predicate> bind(StringPath path, Collection<? extends String> values) {
            return Optional.empty();
        }

        @Override
        public Class<? extends String> valueType(StringPath path) {
            return String.class;
        }
    }

    @Test
    void rejectsDuplicateFilterParams() {
        var factory = createFactoryFor(QCollectionFiltersFactoryTest_TestEntityWithDuplicateNames.testEntityWithDuplicateNames);

        assertThatThrownBy(() -> factory.createFilters())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate filter name 'field1'");
    }

    @Test
    void rejectsMultipleBoundPredicates() {
        var factory = createFactoryFor(
                QCollectionFiltersFactoryTest_TestEntityWithMultipleBoundPredicate.testEntityWithMultipleBoundPredicate);

        assertThatThrownBy(() -> factory.createFilters())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Binding to multiple paths is not supported yet (in %s)".formatted(
                        MultipleBoundPredicate.class
                ));
    }

}