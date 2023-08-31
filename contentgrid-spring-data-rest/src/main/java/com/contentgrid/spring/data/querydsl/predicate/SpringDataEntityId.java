package com.contentgrid.spring.data.querydsl.predicate;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicateException;
import com.contentgrid.spring.querydsl.mapping.UnsupportedCollectionFilterPredicatePathTypeException;
import com.contentgrid.spring.querydsl.predicate.AbstractSimpleQuerydslPredicateFactory;
import com.contentgrid.spring.querydsl.predicate.Default;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor
class SpringDataEntityId extends AbstractSimpleQuerydslPredicateFactory<Path<Object>, Object> implements EntityId {
    private final Repositories repositories;
    private final static Default DEFAULT = new Default();

    @Override
    protected Path<Object> coercePath(Path<?> path) throws UnsupportedCollectionFilterPredicatePathTypeException {
        if(path instanceof CollectionPathBase<?,?,?> collectionPathBase) {
            // If this is on a collection, use the any() variant to search in the collection
            path = (Path<?>) collectionPathBase.any();
        }
        if(path instanceof EntityPathBase<?> entityPathBase) {
            var domainType = entityPathBase.getType();
            var idProperty = repositories.getPersistentEntity(domainType).getIdProperty();
            if(idProperty == null) {
                // If there is no id property, we can't bind to any path
                throw new UnsupportedCollectionFilterPredicateException(this, path, "domain type '%s' has no id property".formatted(domainType));
            } else {
                // Else, navigate to the id property and bind to the path to the id
                return (Path<Object>) new PathNavigator(path).get(idProperty.getName()).getPath();
            }
        }
        throw new UnsupportedCollectionFilterPredicatePathTypeException(this, path, EntityPathBase.class);
    }

    @Override
    protected Optional<Predicate> bindCoerced(Path<Object> path, Collection<?> values) {
        return DEFAULT.bind(path, values);
    }

}
