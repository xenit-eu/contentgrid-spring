package com.contentgrid.spring.data.pagination.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A customization of the {@link org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean} that creates
 * a {@link ContentGridPaginationJpaRepositoryFactory} instead
 * <p>
 * This does not extend the {@link org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean}
 * because we would need to override all the setters anyways (to be able to access them for injection in
 * {@link #doCreateRepositoryFactory()})
 */
public class ContentGridPaginationJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends
        TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private EntityManager entityManager;
    private EntityPathResolver entityPathResolver;
    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
    private JpaQueryMethodFactory queryMethodFactory;
    private JpaQuerydslItemCountStrategy countingStrategy;

    /**
     * Creates a new {@link ContentGridPaginationJpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public ContentGridPaginationJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Autowired
    public void setCountingStrategy(JpaQuerydslItemCountStrategy countingStrategy) {
        this.countingStrategy = countingStrategy;
    }

    /**
     * The {@link EntityManager} to be used.
     *
     * @param entityManager the entityManager to set
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Autowired
    public void setEntityPathResolver(ObjectProvider<EntityPathResolver> resolver) {
        this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
    }

    @Override
    public void setMappingContext(MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
    }

    @Autowired
    public void setQueryMethodFactory(@Nullable JpaQueryMethodFactory factory) {

        if (factory != null) {
            this.queryMethodFactory = factory;
        }
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        Assert.state(entityManager != null, "EntityManager must not be null");

        JpaRepositoryFactory jpaRepositoryFactory = new ContentGridPaginationJpaRepositoryFactory(entityManager,
                countingStrategy);
        jpaRepositoryFactory.setEntityPathResolver(entityPathResolver);
        jpaRepositoryFactory.setEscapeCharacter(escapeCharacter);

        if (queryMethodFactory != null) {
            jpaRepositoryFactory.setQueryMethodFactory(queryMethodFactory);
        }

        return jpaRepositoryFactory;
    }

    public void setEscapeCharacter(char escapeCharacter) {

        this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
    }
}
