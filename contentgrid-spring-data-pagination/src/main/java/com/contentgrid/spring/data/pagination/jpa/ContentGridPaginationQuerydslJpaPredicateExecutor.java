package com.contentgrid.spring.data.pagination.jpa;

import com.contentgrid.spring.data.pagination.ItemCountPageImpl;
import com.contentgrid.spring.data.pagination.jpa.strategy.JpaQuerydslItemCountStrategy;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.util.Assert;

public class ContentGridPaginationQuerydslJpaPredicateExecutor<T> extends QuerydslJpaPredicateExecutor<T> {

    private final EntityPath<T> path;
    private final Querydsl querydsl;
    private final JpaQuerydslItemCountStrategy countingStrategy;

    /**
     * Creates a new {@link ContentGridPaginationQuerydslJpaPredicateExecutor} from the given domain class and {@link EntityManager} and uses
     * the given {@link EntityPathResolver} to translate the domain class into an {@link EntityPath}.
     *
     * @param entityInformation must not be {@literal null}.
     * @param entityManager must not be {@literal null}.
     * @param resolver must not be {@literal null}.
     * @param metadata maybe {@literal null}.
     * @param countingStrategy The counting strategy to use
     */
    public ContentGridPaginationQuerydslJpaPredicateExecutor(
            JpaEntityInformation<T, ?> entityInformation,
            EntityManager entityManager,
            EntityPathResolver resolver,
            CrudMethodMetadata metadata,
            JpaQuerydslItemCountStrategy countingStrategy
    ) {
        super(entityInformation, entityManager, resolver, metadata);
        this.path = resolver.createPath(entityInformation.getJavaType());
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
        this.countingStrategy = countingStrategy;
    }

    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(pageable, "Pageable must not be null");

        // Unpaged means everything will be on the first 'page' anyways.
        // Counts there are automatically done based on size of resultset, without
        // an additional count query.
        if (pageable.isUnpaged()) {
            return super.findAll(predicate, pageable);
        }

        JPQLQuery<T> query = querydsl.applyPagination(pageable, createQuery(predicate).select(path));

        boolean hasNext = false;
        List<T> results;

        // Limit one more than the page size, so we can determine if there is a next page
        query.limit(pageable.getPageSize() + 1);
        var queryResult = query.fetch();
        if (queryResult.size() > pageable.getPageSize()) {
            hasNext = true;
            // Strip off the last item from the result list, so it's not returned as part of the page
            results = queryResult.subList(0, pageable.getPageSize());
        } else {
            results = queryResult;
        }

        return new ItemCountPageImpl<>(
                results,
                pageable,
                hasNext,
                () -> countingStrategy.countQuery(() -> createQuery(predicate).select(path))
        );
    }
}
