package com.contentgrid.spring.data.pagination.jpa;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

import jakarta.persistence.EntityManager;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

public class ContentGridPaginationJpaRepositoryFactory extends JpaRepositoryFactory {

    private final JpaQuerydslItemCountStrategy countingStrategy;

    /**
     * Creates a new {@link ContentGridPaginationJpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     * @param countingStrategy The counting strategy to use for all created repositories
     */
    public ContentGridPaginationJpaRepositoryFactory(EntityManager entityManager,
            JpaQuerydslItemCountStrategy countingStrategy) {
        super(entityManager);
        this.countingStrategy = countingStrategy;
    }

    @Override
    protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata, EntityManager entityManager,
            EntityPathResolver resolver, CrudMethodMetadata crudMethodMetadata) {
        boolean isQueryDslRepository = QUERY_DSL_PRESENT
                && QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

        if (isQueryDslRepository) {

            if (metadata.isReactiveRepository()) {
                throw new InvalidDataAccessApiUsageException(
                        "Cannot combine Querydsl and reactive repository support in a single interface");
            }

            QuerydslJpaPredicateExecutor<?> querydslJpaPredicateExecutor = new ContentGridPaginationQuerydslJpaPredicateExecutor<>(
                    getEntityInformation(metadata.getDomainType()),
                    entityManager,
                    resolver,
                    crudMethodMetadata,
                    countingStrategy
            );

            querydslJpaPredicateExecutor.setProjectionFactory(getProjectionFactory());

            return RepositoryFragments.just(querydslJpaPredicateExecutor);
        }

        return RepositoryFragments.empty();
    }
}
