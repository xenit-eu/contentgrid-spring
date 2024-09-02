package com.contentgrid.spring.querydsl.resolver;

import com.contentgrid.spring.querydsl.converter.CollectionFilterQuerydslPredicateConverter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.CollectionFilteringOperationPredicates;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.OperationPredicates;
import com.contentgrid.thunx.spring.data.querydsl.predicate.injector.resolver.QuerydslPredicateResolver;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
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

    private static final Predicate DEFAULT_PREDICATE = Expressions.TRUE;

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
                .<OperationPredicates>map(CollectionFilteringOperationPredicates::new)
                /*
                If there is no predicate derived from collection filters, we still need to ensure that there is *a* predicate present.
                Without any QueryDSL predicate present, QuerydslRepositoryInvokerAdapterFactory will fall back to the
                default RepositoryInvoker. The default RepositoryInvoker does not process QSort specially, but tries to map the plain Sort.Order objects
                to JPA fields. This will not work in all cases, because these Sort.Order objects are statically derived from OrderSpecifier and not mapped through JPA.
                (It might work for simple cases where there are no special functions or special characters in the name; but that does not cover all cases)
                */
                .or(() -> Optional.of(new CollectionFilteringOperationPredicates(DEFAULT_PREDICATE)));
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
