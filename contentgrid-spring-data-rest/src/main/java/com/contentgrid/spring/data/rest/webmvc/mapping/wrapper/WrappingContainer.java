package com.contentgrid.spring.data.rest.webmvc.mapping.wrapper;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class WrappingContainer implements Container {
    private final Container delegate;
    private final MappingWrapper wrapper;

    @Override
    public TypeInformation<?> getTypeInformation() {
        return delegate.getTypeInformation();
    }

    @Override
    public void doWithProperties(Consumer<Property> handler) {
        delegate.doWithProperties(property -> {
            var wrapped = wrapper.wrapProperty(property);
            handler.accept(new WrappingProperty(wrapped, wrapper));
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        delegate.doWithAssociations(property -> {
            var wrapped = wrapper.wrapProperty(property);
            handler.accept(new WrappingProperty(wrapped, wrapper));
        });
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }
}
