package com.contentgrid.spring.querydsl.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.contentgrid.spring.querydsl.predicate.Default;
import com.querydsl.core.types.Path;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an entity field as being available for filtering the collection of entities
 */
@Target( { METHOD, FIELD })
@Retention(RUNTIME)
@Repeatable(CollectionFilterParams.class)
public @interface CollectionFilterParam {

    /**
     * Special value that indicates that handlers should use the default
     * name (derived from method or field name) for property.
     */
    String USE_DEFAULT_NAME = "";

    /**
     * Defines name of the logical property, i.e. search field
     * name to use for the property. If value is empty String (which is the
     * default), will try to use name of the field that is annotated.
     */
    String value() default USE_DEFAULT_NAME;

    /**
     * To customize the effect this filter has, a {@link QuerydslPredicateFactory} can be specified here.
     * We'll try to obtain a Spring bean of this type,
     * but fall back to plain instantiation if no bean is found in the current BeanFactory.
     */
    Class<? extends QuerydslPredicateFactory> predicate() default Default.class;

    /**
     * Can be set to false to mark the filter parameter as undocumented.
     * <p>
     * Undocumented parameters will be omitted from schemas and descriptions,
     * but they will participate in filtering when they are present
     */
    boolean documented() default true;

}
