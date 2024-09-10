package com.contentgrid.spring.data.pagination.jpa;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.querydsl.jpa.JPQLQuery;

public interface JpaQuerydslItemCountStrategy {

    ItemCount countQuery(JPQLQuery<?> jpqlQuery);
}
