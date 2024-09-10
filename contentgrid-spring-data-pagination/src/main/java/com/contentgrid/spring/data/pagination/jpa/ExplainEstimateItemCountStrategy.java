package com.contentgrid.spring.data.pagination.jpa;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.contentgrid.spring.data.pagination.jpa.hibernate.CountExplainWrappingDialectWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.CacheStoreMode;
import java.util.List;
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
    public ItemCount countQuery(JPQLQuery<?> jpqlQuery) {
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

            var queryPlan = objectMapper.readValue((String) query.getSingleResult(),
                    new TypeReference<List<RootQueryPlan>>() {
                    });

            return new ItemCount(
                    queryPlan.get(0).plan().planRows(),
                    true
            );
        }

        log.warn("No estimated count available, performing a full count for query {}", jpqlQuery);
        return new ItemCount(
                jpqlQuery.fetchCount(),
                false
        );
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
