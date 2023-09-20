package com.contentgrid.spring.querydsl.test.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

@RequiredArgsConstructor
public class PredicateFactoryTester<Q extends EntityPathBase<?>> {
    @Getter
    private final Q pathBase;

    public <P extends Path<?>, T> CuriedPredicateFactory<P, T> evaluate(
            QuerydslPredicateFactory<P, T> factory,
            Function<Q, P> pathFactory
    ) {
        return new CuriedPredicateFactory<>(pathFactory.apply(pathBase), factory);
    }

    public <P extends Path<?>, T> Stream<CuriedPredicateFactory<P, T>> evaluateAll(
            QuerydslPredicateFactory<P, T> factory,
            Stream<Function<Q, P>> pathFactories
    ) {
        return pathFactories.map(pathFactory -> evaluate(factory, pathFactory));
    }

    @RequiredArgsConstructor
    @ToString
    public static class CuriedPredicateFactory<P extends Path<?>, T> {
        @Getter
        private final P path;
        private final QuerydslPredicateFactory<P, T> factory;

        public Optional<Predicate> bindWithConversion(ConversionService conversionService, Collection<String> parameters) {
            var originalType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
            var collectionType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(factory.valueType(path)));
            var convertedParams = (Collection<T>) conversionService.convert(parameters,  originalType, collectionType);
            return bind(convertedParams);
        }

        public Optional<Predicate> bind(Collection<T> parameters) {
            return factory.bind(path, parameters);
        }

        public Stream<Path<?>> boundPaths() {
            return factory.boundPaths(path);
        }
    }

}
