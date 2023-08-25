package com.contentgrid.spring.data.querydsl.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.data.rest.mapping.typeinfo.TypeInformationContainer;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.querydsl.core.types.EntityPath;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

class CollectionFiltersFactoryTest {
    private static CollectionFiltersFactory createFactoryFor(EntityPath<?> path) {
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

    @Test
    void rejectsDuplicateFilterParams() {
        var factory = createFactoryFor(QCollectionFiltersFactoryTest_TestEntityWithDuplicateNames.testEntityWithDuplicateNames);

        assertThatThrownBy(() -> factory.createFilters())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate filter name 'field1'");
    }

}