package com.contentgrid.spring.data.rest.mapping.collectionfilter;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class CollectionFilterBasedProperty implements Property {
    private final Property delegate;
    private final CollectionFilterParam filterParam;

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }

    @Override
    public String getName() {
        return Optional.of(filterParam.value())
                .filter(Predicate.not(Predicate.isEqual(CollectionFilterParam.USE_DEFAULT_NAME)))
                .orElseGet(delegate::getName);
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
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public Optional<Container> nestedContainer() {
        return delegate.nestedContainer()
                .map(CollectionFilterBasedContainer::new);
    }

}
