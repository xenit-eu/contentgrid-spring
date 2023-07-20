package com.contentgrid.spring.data.rest.webmvc.mapping.collectionfilter;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import com.contentgrid.spring.querydsl.annotations.CollectionFilterParam;
import com.contentgrid.spring.querydsl.annotations.CollectionFilterParams;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class CollectionFilterBasedContainer implements Container {
    private final Container delegate;
    private final int maxDepth;

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }

    private static Stream<CollectionFilterParam> getFilterParams(Property property) {
        return property.findAnnotation(CollectionFilterParams.class)
                .map(CollectionFilterParams::value)
                .map(Arrays::stream)
                .or(() -> property.findAnnotation(CollectionFilterParam.class).map(Stream::of))
                .orElseGet(Stream::empty);
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
            getFilterParams(property).forEachOrdered(filterParam -> {
                handler.accept(new CollectionFilterBasedProperty(property, filterParam, maxDepth));
            });
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        if(maxDepth <= 0) {
            return;
        }
        delegate.doWithAssociations(property -> {
            getFilterParams(property).forEachOrdered(filterParam -> {
                handler.accept(new CollectionFilterBasedProperty(property, filterParam, maxDepth));
            });
        });
    }
}
