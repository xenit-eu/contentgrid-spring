package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParams;
import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.querydsl.core.types.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CollectionFiltersFactory {
    @NonNull
    private final PredicateFactoryInstantiator instantiator;

    @NonNull
    private final String prefix;

    @NonNull
    private final Container container;

    @NonNull
    private final PathNavigator pathNavigator;

    public CollectionFilters createFilters() {
        LinkedHashMap<String, CollectionFilter<?>> orderedMap = new LinkedHashMap<>();
        filters().forEachOrdered(filter -> orderedMap.put(filter.getFilterName(), filter));
        return new CollectionFiltersImpl(orderedMap);
    }

    private Stream<CollectionFilter<?>> filters() {
        var filters = Stream.<CollectionFilter<?>>builder();

        container.doWithAll(property -> {
            getFilterParams(property)
                    .flatMap(filterParam -> Stream.concat(
                            this.createFilter(property, filterParam),
                            this.createFactory(property, filterParam)
                                    .flatMap(CollectionFiltersFactory::filters)
                    ))
                    .forEachOrdered(filters);

        });

        return filters.build();

    }

    private Stream<CollectionFilter<?>> createFilter(Property property, CollectionFilterParam filterParam) {
        var propertyPath = pathNavigator.get(property.getName()).getPath();
        QuerydslPredicateFactory<Path<?>, ?> predicateFactory = instantiator.instantiate(filterParam.predicate());

        return predicateFactory.boundPaths(propertyPath)
                .map(boundPath -> new CollectionFilterImpl<>(
                        prefix+getName(property, filterParam),
                        boundPath,
                        filterParam.documented(),
                        propertyPath,
                        (QuerydslPredicateFactory<Path<?>, Object>) predicateFactory
                ));
    }

    private Stream<CollectionFiltersFactory> createFactory(Property property, CollectionFilterParam filterParam) {
        var container = property.nestedContainer();

        if(container.isEmpty()) {
            return Stream.empty();
        }

        var propertyPath = pathNavigator.get(property.getName());
        QuerydslPredicateFactory<Path<?>, ?> predicateFactory = instantiator.instantiate(filterParam.predicate());

        // If there is any mapping at this level, don't descend into the filter's children
        if(predicateFactory.boundPaths(propertyPath.getPath()).findAny().isPresent()) {
            return Stream.of();
        }

        return Stream.of(
                new CollectionFiltersFactory(
                        instantiator,
                        prefix+getName(property, filterParam)+".",
                        container.get(),
                        propertyPath
                )
        );

    }

    private static Stream<CollectionFilterParam> getFilterParams(Property property) {
        return property.findAnnotation(CollectionFilterParams.class)
                .map(CollectionFilterParams::value)
                .map(Arrays::stream)
                .or(() -> property.findAnnotation(CollectionFilterParam.class).map(Stream::of))
                .orElseGet(Stream::empty);
    }

    private static String getName(Property property, CollectionFilterParam param) {
        return Optional.of(param.value())
                .filter(Predicate.not(Predicate.isEqual(CollectionFilterParam.USE_DEFAULT_NAME)))
                .orElseGet(property::getName);
    }
}
