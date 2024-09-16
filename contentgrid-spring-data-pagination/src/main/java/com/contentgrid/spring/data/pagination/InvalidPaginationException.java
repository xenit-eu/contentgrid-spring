package com.contentgrid.spring.data.pagination;

import lombok.Getter;

@Getter
public class InvalidPaginationException extends RuntimeException {

    private final String parameter;

    public InvalidPaginationException(String parameter, String message) {
        super("Invalid parameter '%s': %s".formatted(parameter, message));
        this.parameter = parameter;
    }

    public InvalidPaginationException(String parameter, Throwable cause) {
        this(parameter, cause.getMessage());
        initCause(cause);
    }
}
