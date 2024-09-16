package com.contentgrid.spring.data.pagination.jpa.strategy;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.contentgrid.spring.data.pagination.jpa.hibernate.CountExplainPostgreSQLDialect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.CacheStoreMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.CacheMode;
import org.hibernate.query.Query;

@RequiredArgsConstructor
@Slf4j
public class ExplainEstimateItemCountStrategy implements JpaQuerydslItemCountStrategy {

    private static final String PLACEHOLDER = "placeholder";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SneakyThrows
    public Optional<ItemCount> countQuery(Supplier<JPQLQuery<?>> jpqlQuerySupplier) {
        var jpqlQuery = jpqlQuerySupplier.get();
        if (jpqlQuery instanceof JPAQuery<?> jq) {
            // We need to select a constant *string*, so hibernate expects
            // a string in return (the query plan returned by 'explain')
            var query = jq.select(Expressions.constant(PLACEHOLDER))
                    .createQuery()
                    .unwrap(Query.class);
            query.setCacheable(false);
            query.setCacheMode(CacheMode.IGNORE);
            query.setCacheStoreMode(CacheStoreMode.BYPASS);
            query.setQueryPlanCacheable(false);
            query.addQueryHint(CountExplainPostgreSQLDialect.COUNT_EXPLAIN_HINT);

            String queryPlanString = null;
            try (var resultScroller = query.scroll()) {
                resultScroller.setFetchSize(1);
                while (resultScroller.next()) {
                    queryPlanString = (String) resultScroller.get();
                    if (Objects.equals(PLACEHOLDER, queryPlanString)) {
                        // We got a placeholder instead of a query plan; we can't do anything with this
                        log.warn("Count explain hibernate dialect is not installed. Can not perform an estimate count");
                        return Optional.empty();
                    }
                }
            }
            if (queryPlanString == null) {
                log.warn("No query plan was returned from query. Can not perform estimate count");
                return Optional.empty();
            }

            var queryPlan = objectMapper.readValue(queryPlanString,
                    new TypeReference<List<RootQueryPlan>>() {
                    });

            return Optional.of(ItemCount.estimated(queryPlan.get(0).plan().planRows()));
        }
        return Optional.empty();
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RootQueryPlan(
            @JsonProperty("Plan")
            QueryPlan plan
    ) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QueryPlan(
            @JsonProperty("Plan Rows")
            long planRows
    ) {

    }
}
