package com.contentgrid.spring.data.pagination.jpa.strategy;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.QueryTimeoutException;
import java.util.Optional;
import java.util.function.Supplier;
import org.hibernate.query.Query;

public class TimedDirectCountItemCountStrategy implements JpaQuerydslItemCountStrategy {

    @Override
    public Optional<ItemCount> countQuery(Supplier<JPQLQuery<?>> jpqlQuerySupplier) {
        var jpqlQuery = jpqlQuerySupplier.get();
        if (jpqlQuery instanceof JPAQuery<?> jpaQuery) {
            var query = jpaQuery.select(Expressions.numberTemplate(Long.class, "count(*)"))
                    .createQuery()
                    .unwrap(Query.class);
            query.setTimeout(1); // timeout in seconds; hibernate rounds the JPA timeout hint to seconds anyways

            try {
                long count = (long) query.getSingleResult();
                return Optional.of(new ItemCount(
                        count,
                        false
                ));
            } catch (QueryTimeoutException ex) {
                // Query timed out; no count (return below)
            }

        }
        return Optional.empty();
    }
}
