package com.contentgrid.spring.querydsl.resolver;

import com.contentgrid.spring.querydsl.converter.CollectionFilterQuerydslPredicateConverter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.CollectionFilteringOperationPredicates;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.OperationPredicates;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.QuerydslPredicateResolver;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RequiredArgsConstructor
public class CollectionFilterParamPredicateResolver implements QuerydslPredicateResolver {

    private final CollectionFilterQuerydslPredicateConverter querydslPredicateConverter;

    public CollectionFilterParamPredicateResolver(CollectionFiltersMapping filtersMapping, ConversionService conversionService) {
        this(new CollectionFilterQuerydslPredicateConverter(filtersMapping, conversionService));
    }

    @Override
    public Optional<OperationPredicates> resolve(MethodParameter methodParameter, Class<?> domainType,
            Map<String, String[]> parameters) {
        if(!methodParameter.hasParameterAnnotation(QuerydslPredicate.class)) {
            return Optional.empty();
        }

        return querydslPredicateConverter.getPredicate(domainType, toMultiValueMap(parameters))
                .map(CollectionFilteringOperationPredicates::new);
    }

    /**
     * Converts the given Map into a {@link MultiValueMap}.
     *
     * @param source must not be {@literal null}.
     * @return the converted {@link MultiValueMap}.
     */
    private static MultiValueMap<String, String> toMultiValueMap(Map<String, String[]> source) {

        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();

        for (Entry<String, String[]> entry : source.entrySet()) {
            result.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }

        return result;
    }
}
