package com.contentgrid.spring.querydsl.mapping;

import lombok.Getter;

/**
 * Base exception for invalid <b>values</b> for a {@link CollectionFilter}
 *
 * Exceptions with this base class should only be thrown for invalid values supplied by <i>consumers</i> of the API.
 */
@Getter
public abstract class InvalidCollectionFilterValueException extends RuntimeException {

    private final CollectionFilter<?> filter;
    private final Object invalidValue;

    protected InvalidCollectionFilterValueException(String message, CollectionFilter<?> filter, Object invalidValue) {
        super(createMessage(filter, invalidValue) + ": " + message);
        this.filter = filter;
        this.invalidValue = invalidValue;
    }

    private static String createMessage(CollectionFilter<?> filter, Object invalidValue) {
        if (invalidValue != null) {
            return "Filter '%s' with value [%s]".formatted(filter.getFilterName(), invalidValue);
        } else {
            return "Filter '%s' with null value".formatted(filter.getFilterName());
        }
    }
}
