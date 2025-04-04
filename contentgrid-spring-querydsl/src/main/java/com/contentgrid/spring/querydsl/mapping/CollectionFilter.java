package com.contentgrid.spring.querydsl.mapping;

import com.contentgrid.spring.querydsl.annotation.QuerydslPredicateFactory;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Optional;

/**
 * Operational representation of a {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam} annotation
 * <p>
 * This operational representation can be resolved from a {@link CollectionFiltersMapping}. A collection filter can be
 * used to filter a collection of entities. Depending on the concrete implementation, it can also be used for sorting
 * the collection.
 *
 * @param <T> Type of the path referenced by the filter
 */
public interface CollectionFilter<T> {

    /**
     * Obtain the name of the filter
     * <p>
     * The name of the filter is used literally as a request query parameter when filtering a collection. It is also
     * used literally as the property to sort on when using the <pre>sort</pre> parameter for sorting a collection.
     *
     * @return The name of the filter
     */
    String getFilterName();

    /**
     * Obtain the type of the filter
     * <p>
     * API clients can use the type name to know which search type belongs to each query parameter.
     *
     * @return The type of the filter
     */
    String getFilterType();

    /**
     * Whether this filter is documented or not
     * <p>
     * Documented filter parameters are present in schemas and descriptions, undocumented parameters are hidden from
     * them.
     * <p>
     * Filter and sort functionality is active regardless of its documentation state.
     *
     * @return Whether this filter is documented or not
     */
    boolean isDocumented();

    /**
     * Obtain the QueryDSL Path that is used by this filter
     * <p>
     * This method is primarily useful for reverse lookups from Path back to a filter
     */
    Path<T> getPath();

    Class<? extends QuerydslPredicateFactory<Path<?>, ?>> getPredicateFactoryClass();

    /**
     * Obtain the Java property that was annotated with
     * {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam}
     *
     * @return The element that is annotated with
     * {@link com.contentgrid.spring.querydsl.annotation.CollectionFilterParam}
     */
    AnnotatedElement getAnnotatedElement();

    /**
     * Obtain the type of the request query parameter(s) for this filter
     * <p>
     * Since query parameters are always strings in their raw format, they may need to be converted to this type before
     * being used in {@link #createPredicate(Collection)}
     *
     * @return The type to convert the request query parameter(s) to
     */
    Class<T> getParameterType();

    /**
     * Creates a filter predicate from request query parameters
     *
     * @param parameters The query parameters that are converted to the correct type as given by
     * {@link #getParameterType()}
     * @return A predicate if one can be created from the parameters
     */
    Optional<Predicate> createPredicate(Collection<T> parameters);

    /**
     * Creates a sort order specification from the <code>sort</code> request query parameter
     * <p>
     * Not all types of collection filter support sorting. If sorting is not supported, an empty optional will be
     * returned.
     *
     * @param order The direction the property should be sorted in
     * @return An order specification if one can be created for the specified order and collection filter
     */
    Optional<OrderSpecifier<?>> createOrderSpecifier(Order order);
}
