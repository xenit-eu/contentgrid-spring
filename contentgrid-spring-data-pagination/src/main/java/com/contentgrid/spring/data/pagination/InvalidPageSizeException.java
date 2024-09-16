package com.contentgrid.spring.data.pagination;

public class InvalidPageSizeException extends InvalidPaginationException {

    public InvalidPageSizeException(String parameter, String message) {
        super(parameter, message);
    }

    public InvalidPageSizeException(String parameter, Throwable cause) {
        this(parameter, cause.getMessage());
        initCause(cause);
    }

    public static InvalidPageSizeException mustBePositive(String parameter) {
        return new InvalidPageSizeException(parameter, "must be positive");
    }
}
