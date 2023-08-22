package com.contentgrid.spring.data.rest.mapping.persistent;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class ThroughAssociationsContainer implements Container {
    private final Container delegate;
    private final Repositories repositories;
    private final int maxDepth;

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
        if(maxDepth <= 0) {
            return;
        }
        delegate.doWithProperties(property -> {
                handler.accept(new ThroughAssociationsProperty(property, repositories, maxDepth));
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        if(maxDepth <= 0) {
            return;
        }
        delegate.doWithAssociations(property -> {
            handler.accept(new ThroughAssociationsProperty(property, repositories, maxDepth));
        });
    }
}
