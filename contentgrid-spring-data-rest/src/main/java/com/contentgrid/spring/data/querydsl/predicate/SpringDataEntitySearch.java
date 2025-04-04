package com.contentgrid.spring.data.querydsl.predicate;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.querydsl.predicate.Default;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.querydsl.predicate.EntitySearch;
import com.contentgrid.spring.querydsl.predicate.Text;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor
@Slf4j
public class SpringDataEntitySearch implements QuerydslPredicateFactory<Path<?>, Object>, EntitySearch {

    private final CollectionFiltersMapping collectionFiltersMapping;

    private final ConversionService defaultConversionService;

    private final Repositories repositories;

    private final QuerydslBindingsFactory querydslBindingsFactory;

    private static final Set<Class<? extends QuerydslPredicateFactory<Path<?>, ?>>> SUPPORTED_PREDICATES = Set.of(
            Default.class, EntityId.class, Text.EqualsNormalized.class, Text.StartsWith.class, Text.StartsWithNormalized.class,
            Text.EqualsIgnoreCase.class, Text.EqualsIgnoreCaseNormalized.class, Text.StartsWithIgnoreCase.class,
            Text.StartsWithIgnoreCaseNormalized.class, Text.ContentGridPrefixSearch.class
    );

    @Override
    public Stream<Path<?>> boundPaths(Path<?> path) {
        return Stream.of(path);
    }

    @Override
    public Optional<Predicate> bind(Path<?> path, Collection<?> values) {
        var domainType = path.getRoot().getType();
        var mainPathNavigator = new PathNavigator(querydslBindingsFactory.getEntityPathResolver().createPath(domainType));
        var predicateBuilder = buildPredicateForDomainType(domainType, values);
        var entity = repositories.getPersistentEntity(domainType);
        entity.doWithAssociations((SimpleAssociationHandler) property -> {
            var targetType = property.getInverse().getAssociationTargetType();
            var builder = buildPredicateForDomainType(targetType, values);
            var entityPath = querydslBindingsFactory.getEntityPathResolver().createPath(targetType);
            if (builder.getValue() != null) {
                var pathNavigator = new PathNavigator(entityPath);
                // TODO: use getIdProperty().getName() instead of "id"
                var sourceRelationPath = (Path<Object>) mainPathNavigator.get(property.getInverse().getName()).get("id").getPath();
                var targetRelationPath = (Path<Object>) pathNavigator.get("id").getPath();
                predicateBuilder.or(JPAExpressions.selectOne()
                        .from(entityPath)
                        .where(ExpressionUtils.and(builder.getValue(),
                                ExpressionUtils.eq(sourceRelationPath, targetRelationPath)))
                        .exists());
            }
        });
        return Optional.ofNullable(predicateBuilder.getValue());
    }

    private BooleanBuilder buildPredicateForDomainType(Class<?> domainType, Collection<?> values) {
        var filters = collectionFiltersMapping.forDomainType(domainType);
        var predicateBuilder = new BooleanBuilder();
        filters.filters()
                // Remove predicates over associations
                .filter(filter -> Objects.equals(filter.getPath().getMetadata().getParent(), filter.getPath().getRoot()))
                // Check if type supported
                .filter(filter -> SUPPORTED_PREDICATES.stream().anyMatch(type -> type.isAssignableFrom(filter.getPredicateFactoryClass())))
                .forEach(filter -> {
                    Collection<?> typedParameters;
                    try {
                        typedParameters = convertParametersToType(filter.getParameterType(), values);
                        ((CollectionFilter<Object>) filter)
                                .createPredicate((Collection<Object>) typedParameters)
                                .ifPresent(predicateBuilder::or);
                    } catch (ConversionFailedException e) {
                        log.debug("Conversion failed for filter {}", filter.getFilterName(), e);
                    }
                });
        return predicateBuilder;
    }

    private <T> Collection<T> convertParametersToType(Class<T> targetType, Collection<?> params) {
        return params.stream()
                .map(value -> defaultConversionService.convert(value, targetType))
                .toList();
    }

    @Override
    public Class<Object> valueType(Path<?> path) {
        return Object.class;
    }

    @Override
    public String getFilterType() {
        return null;
    }
}
