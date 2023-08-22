package com.contentgrid.spring.data.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;

@RequiredArgsConstructor
public class BeanFactoryPredicateFactoryInstantiator implements PredicateFactoryInstantiator {
    private final BeanFactory beanFactory;
    private final PredicateFactoryInstantiator fallback;

    @Override
    public QuerydslPredicateFactory<Path<?>, ?> instantiate(Class<? extends QuerydslPredicateFactory<Path<?>, ?>> clazz) {
        Class c = clazz;
        return (QuerydslPredicateFactory<Path<?>, ?>) beanFactory.getBeanProvider(c)
                .getIfAvailable(() -> fallback.instantiate(clazz));
    }
}
