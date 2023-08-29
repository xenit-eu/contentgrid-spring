package com.contentgrid.spring.querydsl.converter;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import lombok.Getter;
import lombok.experimental.StandardException;

public class InvalidCollectionFilterValueException extends RuntimeException {
    @Getter
    private final CollectionFilter filter;

    @Getter
    private final String invalidValue;

    public InvalidCollectionFilterValueException(CollectionFilter filter, String invalidValue) {
        super(createMessage(filter, invalidValue));
        this.filter = filter;
        this.invalidValue = invalidValue;
    }

    public InvalidCollectionFilterValueException(CollectionFilter filter, String invalidValue, Throwable cause) {
        this(filter, invalidValue);
        initCause(cause);
    }

    private static String createMessage(CollectionFilter filter, String invalidValue) {
        return "Failed to convert value for filter '%s' to type '%s' for value [%s]".formatted(
                filter.getFilterName(),
                filter.getPath().getType(),
                invalidValue
        );
    }
}
