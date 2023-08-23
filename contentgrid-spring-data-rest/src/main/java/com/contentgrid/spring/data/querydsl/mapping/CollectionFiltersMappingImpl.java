package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.data.rest.mapping.persistent.PersistentEntityContainer;
import com.contentgrid.spring.data.rest.mapping.persistent.ThroughAssociationsContainer;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFilters;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor
@Slf4j
class CollectionFiltersMappingImpl implements CollectionFiltersMapping {
    private final Repositories repositories;
    private final PredicateFactoryInstantiator predicateFactoryInstantiator;
    private final EntityPathResolver entityPathResolver;
    private final int maxDepth;


    private final ConcurrentMap<PersistentEntity<?, ?>, CollectionFilters> cache = new ConcurrentHashMap<>();

    @Override
    public CollectionFilters forDomainType(Class<?> domainType) {
        var persistentEntity = repositories.getPersistentEntity(domainType);
        return forPersistentEntity(persistentEntity);
    }

    @Override
    public Optional<CollectionFilter> forProperty(Class<?> domainType, String... properties) {
        var persistentEntity = repositories.getPersistentEntity(domainType);

        var pathNavigator = createEntityPathNavigatorFor(persistentEntity);
        for (String propertyName : properties) {
            pathNavigator = pathNavigator.get(propertyName);
        }

        return forPersistentEntity(persistentEntity).forPath(pathNavigator.getPath()).filters().findFirst();
    }

    @Override
    public Optional<CollectionFilter> forIdProperty(Class<?> domainType, String... properties) {
        var persistentEntity = repositories.getPersistentEntity(domainType);

        var pathNavigator = createEntityPathNavigatorFor(persistentEntity);
        for (String propertyName : properties) {
            pathNavigator = pathNavigator.get(propertyName);
        }

        var targetDomainType = pathNavigator.getTargetType();

        var targetIdProperty = repositories.getPersistentEntity(targetDomainType)
                .getRequiredIdProperty()
                .getName();

        pathNavigator = pathNavigator.get(targetIdProperty);

        return forPersistentEntity(persistentEntity).forPath(pathNavigator.getPath()).filters().findFirst();
    }

    private CollectionFilters forPersistentEntity(PersistentEntity<?, ?> persistentEntity) {
        return cache.computeIfAbsent(persistentEntity, this::createCollectionFilters);
    }

    private CollectionFilters createCollectionFilters(PersistentEntity<?, ?> persistentEntity) {
        var pathNavigator = createEntityPathNavigatorFor(persistentEntity);

        var creator = new CollectionFiltersFactory(
                predicateFactoryInstantiator,
                "",
                new ThroughAssociationsContainer(new PersistentEntityContainer(persistentEntity), repositories, maxDepth),
                pathNavigator
        );

        var filters = creator.createFilters();
        if(log.isDebugEnabled()) {
            log.debug("Built CollectionFilters for {}: {}",
                    persistentEntity.getType(),
                    filters.filters().map(CollectionFilter::getFilterName).collect(Collectors.toList())
            );
        }
        return filters;
    }

    private PathNavigator createEntityPathNavigatorFor(PersistentEntity<?, ?> persistentEntity) {
        var domainType = persistentEntity.getType();
        var entityPath = entityPathResolver.createPath(domainType);
        return new PathNavigator(entityPath);
    }
}
