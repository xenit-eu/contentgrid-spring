package com.contentgrid.spring.data.pagination.jpa;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.querydsl.jpa.JPQLQuery;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AggregateItemCountStrategy implements JpaQuerydslItemCountStrategy {

    private final List<JpaQuerydslItemCountStrategy> delegates;

    @Override
    public Optional<ItemCount> countQuery(Supplier<JPQLQuery<?>> jpqlQuery) {
        for (var delegate : delegates) {
            var result = delegate.countQuery(jpqlQuery);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

}
