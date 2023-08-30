package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import org.springframework.beans.BeanUtils;

class DirectPredicateFactoryInstantiator implements PredicateFactoryInstantiator {
    @Override
    @SuppressWarnings("unchecked")
    public QuerydslPredicateFactory<? extends Path<?>, ?> instantiate(Class<? extends QuerydslPredicateFactory> clazz) {
        return BeanUtils.instantiateClass((Class<? extends QuerydslPredicateFactory<Path<?>,?>>) clazz);
    }
}
