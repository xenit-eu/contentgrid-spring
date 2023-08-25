package com.contentgrid.spring.data.querydsl.predicate;

import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.querydsl.predicate.Default;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor
class SpringDataEntityId implements EntityId {
    private final Repositories repositories;
    private final static Default DEFAULT = new Default();

    @Override
    public Stream<Path<?>> boundPaths(Path<?> path) {
        if(path instanceof CollectionPathBase<?,?,?> collectionPathBase) {
            // If this is on a collection, use the any() variant to search in the collection
            path = (Path<?>) collectionPathBase.any();
        }
        if(path instanceof EntityPathBase<?> entityPathBase) {
            var domainType = entityPathBase.getType();
            var idProperty = repositories.getPersistentEntity(domainType).getIdProperty();
            if(idProperty == null) {
                // If there is no id property, we don't bind to any path
                return Stream.empty();
            } else {
                // Else, navigate to the id property and bind to the path to the id
                return Stream.of(new PathNavigator(path).get(idProperty.getName()).getPath());
            }
        }
        return Stream.empty();
    }

    @Override
    public Optional<Predicate> bind(Path<?> path, Collection<?> values) {
        return DEFAULT.bind(path, values);
    }

}
