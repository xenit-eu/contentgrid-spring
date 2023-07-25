package com.contentgrid.spring.data.rest.webmvc.mapping;

import java.lang.annotation.Annotation;
import java.util.Optional;
import org.springframework.data.util.TypeInformation;

public interface Property {
    String getName();
    TypeInformation<?> getTypeInformation();

    boolean isIgnored();
    boolean isRequired();
    boolean isReadOnly();

    Optional<Container> nestedContainer();

    <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass);
}
