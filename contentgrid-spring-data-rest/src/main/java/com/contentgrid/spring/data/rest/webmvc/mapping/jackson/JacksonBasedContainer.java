package com.contentgrid.spring.data.rest.webmvc.mapping.jackson;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.data.util.TypeInformation;

@AllArgsConstructor
public class JacksonBasedContainer implements Container{
    private final Container delegate;

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        return delegate.getTypeInformation();
    }

    @Override
    public void doWithProperties(Consumer<Property> handler) {
        delegate.doWithProperties(property -> {
            handler.accept(new JacksonBasedProperty(property));
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        delegate.doWithAssociations(property -> {
            handler.accept(new JacksonBasedProperty(property));
        });
    }
}
