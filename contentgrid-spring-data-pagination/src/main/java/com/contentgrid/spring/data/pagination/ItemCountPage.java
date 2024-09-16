package com.contentgrid.spring.data.pagination;

import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * A sublist of objects, including information about the total number of items in the form of {@link ItemCount}
 */
public interface ItemCountPage<T> extends Page<T> {

    /**
     * @return The count for the total number of elements
     */
    ItemCount getTotalItemCount();

    /**
     * {@inheritDoc}
     */
    @Override
    <U> ItemCountPage<U> map(Function<? super T, ? extends U> converter);
}
