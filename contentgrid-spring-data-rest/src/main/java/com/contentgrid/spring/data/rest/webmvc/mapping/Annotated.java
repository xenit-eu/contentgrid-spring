package com.contentgrid.spring.data.rest.webmvc.mapping;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface Annotated {
    <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass);
}
