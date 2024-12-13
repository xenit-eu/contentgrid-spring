package com.contentgrid.spring.data.rest.mapping.persistent;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class ThroughAssociationsProperty implements Property {
    private final Property delegate;
    private final Repositories repositories;
    private final int maxDepth;

    private final static Set<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS = Set.of(
            OneToOne.class,
            OneToMany.class,
            ManyToOne.class,
            ManyToMany.class
    );

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        return delegate.getTypeInformation();
    }

    @Override
    public boolean isIgnored() {
        return delegate.isIgnored();
    }

    @Override
    public boolean isRequired() {
        return delegate.isRequired();
    }

    @Override
    public boolean isUnique() {
        return delegate.isUnique();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public Optional<Container> nestedContainer() {
        return delegate.nestedContainer()
                .<Container>map(container -> new ThroughAssociationsContainer(container, repositories, maxDepth))
                .or(this::relationContainer);
    }

    private Optional<Container> relationContainer() {
        if(ASSOCIATION_ANNOTATIONS.stream()
                .anyMatch(annotation -> delegate.findAnnotation(annotation).isPresent())) {
            var persistentEntity = repositories.getPersistentEntity(delegate.getTypeInformation().getRequiredActualType().getType());
            return Optional.of(new ThroughAssociationsContainer(new PersistentEntityContainer(persistentEntity), repositories, maxDepth-1));
        }
        return Optional.empty();
    }
}
