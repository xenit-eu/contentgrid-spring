package com.contentgrid.spring.data.rest.mapping;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.data.util.TypeInformation;

public interface Container {
    TypeInformation<?> getTypeInformation();
    default void doWithAll(Consumer<Property> handler) {
        doWithProperties(handler);
        doWithAssociations(handler);
    }
    void doWithProperties(Consumer<Property> handler);
    void doWithAssociations(Consumer<Property> handler);

    <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass);
}
