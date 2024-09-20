package com.contentgrid.spring.data.pagination;

public class InvalidPageSizeException extends InvalidPaginationException {

    public InvalidPageSizeException(String parameter, String invalidValue, String message) {
        super(parameter, invalidValue, message);
    }

    public InvalidPageSizeException(String parameter, String invalidValue, Throwable cause) {
        this(parameter, invalidValue, cause.getMessage());
        initCause(cause);
    }

    public static InvalidPageSizeException mustBePositive(String parameter, String invalidValue) {
        return new InvalidPageSizeException(parameter, invalidValue, "must be positive");
    }
}
