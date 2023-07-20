package com.contentgrid.spring.data.rest.webmvc.mapping;

import com.contentgrid.spring.data.rest.webmvc.mapping.persistent.PersistentEntityContainer;
import java.util.Iterator;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;

@RequiredArgsConstructor
public class DomainTypeMapping implements Iterable<Class<?>> {
    private final Repositories repositories;
    private final Function<Container, Container> factory;

    @Override
    public Iterator<Class<?>> iterator() {
        return repositories.iterator();
    }

    public Container forDomainType(Class<?> domainType) {
        var persistentEntity = repositories.getPersistentEntity(domainType);
        return forPersistentEntity(persistentEntity);
    }

    private Container forPersistentEntity(PersistentEntity<?, ?> entity) {
        return factory.apply(new PersistentEntityContainer(entity));
    }
}
