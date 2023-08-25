package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;

/**
 * Creates a {@link com.querydsl.core.types.Predicate} mapping to the id of the association.
 *
 * This predicate type can only be used on associations, not on basic properties or on embedded objects
 */
public interface EntityId extends QuerydslPredicateFactory<Path<? extends Object>, Object> {

}
