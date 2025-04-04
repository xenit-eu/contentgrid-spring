package com.contentgrid.spring.querydsl.predicate;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;

/**
 * Creates a {@link com.querydsl.core.types.Predicate} that searches across all properties of the entity (including associations).
 */
public interface EntitySearch extends QuerydslPredicateFactory<Path<?>, Object> {

}
