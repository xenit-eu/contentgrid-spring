package com.contentgrid.spring.data.pagination;

import lombok.Getter;

@Getter
public class InvalidPaginationException extends RuntimeException {

    private final String parameter;

    private final String invalidValue;

    public InvalidPaginationException(String parameter, String invalidValue, String message) {
        super("Invalid parameter '%s': %s".formatted(parameter, message));
        this.parameter = parameter;
        this.invalidValue = invalidValue;
    }

    public InvalidPaginationException(String parameter, String invalidValue, Throwable cause) {
        this(parameter, invalidValue, cause.getMessage());
        initCause(cause);
    }
}
