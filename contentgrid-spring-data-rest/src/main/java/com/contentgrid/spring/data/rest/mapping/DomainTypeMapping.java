package com.contentgrid.spring.data.rest.mapping;

import com.contentgrid.spring.data.rest.mapping.persistent.PersistentEntityContainer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainTypeMapping implements Iterable<Class<?>> {
    private final Repositories repositories;
    private final Map<PersistentEntity<?, ?>, Container> containerCache;
    private final Function<Container, Container> factory;

    public DomainTypeMapping(Repositories repositories) {
        this(repositories, new ConcurrentHashMap<>(), Function.identity());
    }

    public DomainTypeMapping wrapWith(Function<Container, Container> factory) {
        return new DomainTypeMapping(repositories, containerCache, this.factory.andThen(factory));
    }

    @Override
    public Iterator<Class<?>> iterator() {
        return repositories.iterator();
    }

    public Container forDomainType(Class<?> domainType) {
        var persistentEntity = repositories.getPersistentEntity(domainType);
        return forPersistentEntity(persistentEntity);
    }

    private Container forPersistentEntity(PersistentEntity<?, ?> entity) {
        return factory.apply(this.containerCache.computeIfAbsent(entity, PersistentEntityContainer::new));
    }
}
