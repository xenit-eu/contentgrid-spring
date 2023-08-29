package com.contentgrid.spring.querydsl.converter;

import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

@RequiredArgsConstructor
public class CollectionFilterQuerydslPredicateConverter {
    private final CollectionFiltersMapping collectionFiltersMapping;
    private final ConversionService conversionService;

    public Optional<Predicate> getPredicate(Class<?> domainType, Map<String, ? extends Collection<String>> parameters) {
        var mapping = collectionFiltersMapping.forDomainType(domainType);

        var predicateBuilder = new BooleanBuilder();

        parameters.forEach((paramName, paramValues) -> {
            mapping.named(paramName).ifPresent(filter -> {
                Collection<?> typedParameters;
                try {
                     typedParameters = convertParametersToType(filter.getPath().getType(), paramValues);
                } catch(ConversionFailedException e) {
                    throw new InvalidCollectionFilterValueException(filter, (String)e.getValue(), e);
                }
                filter.createPredicate(typedParameters).ifPresent(predicateBuilder::and);
            });
        });

        return Optional.ofNullable(predicateBuilder.getValue());
    }

    private Collection<?> convertParametersToType(Class<?> targetType, Collection<String> params) {
        if(conversionService.canConvert(String.class, targetType)) {
            return params.stream()
                    .map(value -> conversionService.convert(value, targetType))
                    .toList();
        } else {
            return params;
        }
    }

}
