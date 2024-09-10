package com.contentgrid.spring.data.pagination.jpa;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.querydsl.jpa.JPQLQuery;
import java.util.Optional;

public interface JpaQuerydslItemCountStrategy {

    Optional<ItemCount> countQuery(JPQLQuery<?> jpqlQuery);
}
