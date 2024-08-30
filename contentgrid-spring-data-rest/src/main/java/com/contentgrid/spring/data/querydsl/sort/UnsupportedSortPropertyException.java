package com.contentgrid.spring.data.querydsl.sort;

import lombok.Getter;
import org.springframework.data.domain.Sort.Order;

/**
 * Exception for when sorting is requested in an unrecognized property or a property that does not support sorting.
 * <p>
 * This exception should only be thrown for invalid values supplied by <i>consumers</i> of the REST API.
 */
@Getter
public class UnsupportedSortPropertyException extends RuntimeException {
    private final Order order;

    public UnsupportedSortPropertyException(Order order) {
        super("Sort parameter '%s' is not supported".formatted(order.getProperty()));
        this.order = order;
    }
}
