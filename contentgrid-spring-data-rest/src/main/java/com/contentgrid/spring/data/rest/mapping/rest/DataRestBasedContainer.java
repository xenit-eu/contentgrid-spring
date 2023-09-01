package com.contentgrid.spring.data.rest.mapping.rest;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class DataRestBasedContainer implements Container {
    private final Container delegate;

    @Override
    public TypeInformation<?> getTypeInformation() {
        return delegate.getTypeInformation();
    }

    @Override
    public void doWithProperties(Consumer<Property> handler) {
        delegate.doWithProperties(property -> {
            handler.accept(new DataRestBasedProperty(property));
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        delegate.doWithAssociations(property -> {
            handler.accept(new DataRestBasedProperty(property));
        });
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }
}
