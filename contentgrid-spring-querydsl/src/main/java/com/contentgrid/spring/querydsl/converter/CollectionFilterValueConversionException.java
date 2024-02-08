package com.contentgrid.spring.querydsl.converter;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.InvalidCollectionFilterValueException;
import org.springframework.core.convert.ConversionFailedException;

/**
 * A failure to convert a value to the type expected by the {@link CollectionFilter}
 */
public class CollectionFilterValueConversionException extends InvalidCollectionFilterValueException {

    public CollectionFilterValueConversionException(CollectionFilter<?> filter, Object invalidValue,
            ConversionFailedException cause) {
        super(createMessage(cause), filter, invalidValue);
        initCause(cause);
    }

    private static String createMessage(ConversionFailedException conversionFailedException) {
        return "Failed to convert to type '%s'".formatted(conversionFailedException.getTargetType());
    }

    @Override
    public synchronized ConversionFailedException getCause() {
        return (ConversionFailedException) super.getCause();
    }
}
