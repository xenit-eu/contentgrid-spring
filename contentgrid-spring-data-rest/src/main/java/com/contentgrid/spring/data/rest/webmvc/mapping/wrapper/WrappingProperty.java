package com.contentgrid.spring.data.rest.webmvc.mapping.wrapper;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class WrappingProperty implements Property {
    private final Property delegate;
    private final MappingWrapper wrapper;

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
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public Optional<Container> nestedContainer() {
        return delegate.nestedContainer()
                .map(wrapper::wrapContainer)
                .map(container -> new WrappingContainer(container, wrapper));
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }
}
