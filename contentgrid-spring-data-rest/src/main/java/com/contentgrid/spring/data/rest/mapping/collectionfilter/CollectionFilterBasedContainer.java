package com.contentgrid.spring.data.rest.mapping.collectionfilter;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParams;
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
        delegate.doWithProperties(property -> {
            getFilterParams(property).forEachOrdered(filterParam -> {
                handler.accept(new CollectionFilterBasedProperty(property, filterParam));
            });
        });
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        delegate.doWithAssociations(property -> {
            getFilterParams(property).forEachOrdered(filterParam -> {
                handler.accept(new CollectionFilterBasedProperty(property, filterParam));
            });
        });
    }
}
