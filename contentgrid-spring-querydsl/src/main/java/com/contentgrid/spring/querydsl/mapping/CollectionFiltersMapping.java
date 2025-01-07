package com.contentgrid.spring.querydsl.mapping;

import java.util.Optional;

/**
 * Global mapping of Spring Data entities to {@link CollectionFilter}s
 */
public interface CollectionFiltersMapping {

    /**
     * Retrieves all {@link CollectionFilter}s that are defined for an entity
     *
     * @param domainType The entity class
     * @return All collection filters defined for the entity
     */
    CollectionFilters forDomainType(Class<?> domainType);

    /**
     * Locates the {@link CollectionFilter}s that can be used for filtering on a particular property
     * <p>
     * Collection filters for a property can be located e.g. {@code forProperty(Invoice.class, "number")}
     * <p>
     * Collection filters for nested properties or that apply across relations can also be located e.g.
     * {@code forProperty(Invoice.class, "pdf", "mimetype")} or {@code forProperty(Invoice.class", "customer", "name")}
     * <p>
     * These examples assume an {@code Invoice} entity class with a "number" property, a "pdf" property that is
     * {@code @Embedded} and a "customer" property that is a {@code @ManyToOne} relation.
     *
     * @param domainType The entity class
     * @param properties Path of Java property names to follow to arrive at the property
     * @return The first collection filter for the property, if there is any
     */
    CollectionFilters forProperty(Class<?> domainType, String ...properties);

    /**
     * Locates the {@link CollectionFilter} that can be used for filtering on the id of a related entity
     * <p>
     * Only collection filters that apply across relations can be located using this function. In this case, the
     * property path needs to point to the relation directly. e.g. {@code forIdProperty(Invoice.class, "customer")}
     * <p>
     * This example assumes an {@code Invoice} entity class with a "customer" property that is a {@code @ManyToOne}
     * relation.
     *
     * @param domainType The entity class
     * @param properties Path of Java property names to follow to arrive at the relation
     * @return The first collection filter for the {@code @Id} of the entity on the other side of the relation
     */
    Optional<CollectionFilter<?>> forIdProperty(Class<?> domainType, String... properties);
}
