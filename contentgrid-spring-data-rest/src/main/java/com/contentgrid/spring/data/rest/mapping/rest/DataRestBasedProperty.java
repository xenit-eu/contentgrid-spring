package com.contentgrid.spring.data.rest.mapping.rest;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class DataRestBasedProperty implements Property {
    private final Property delegate;

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
        if(delegate.isIgnored()) {
            return true;
        }
        return findAnnotation(RestResource.class)
                .map(restResource -> !restResource.exported())
                .orElse(false);
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
                .map(DataRestBasedContainer::new);
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }
}
