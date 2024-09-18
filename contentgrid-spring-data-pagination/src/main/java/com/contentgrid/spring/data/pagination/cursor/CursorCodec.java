package com.contentgrid.spring.data.pagination.cursor;

import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.StandardException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponents;

/**
 * Encoder and decoder for pagination query parameters
 */
public interface CursorCodec {

    /**
     * Decodes a cursor to a spring pageable
     *
     * @param context The cursor to decode
     * @param uriComponents The rest of the URI, without cursor, page size or sort parameters
     * @return Spring pageable, decoded from the cursor
     * @throws CursorDecodeException When a cursor can not be decoded
     */
    Pageable decodeCursor(CursorContext context, UriComponents uriComponents) throws CursorDecodeException;

    /**
     * Encodes a spring pageable to a cursor
     *
     * @param pageable The spring pageable
     * @param uriComponents The rest of the URI, without cursor, page size or sort parameters
     * @return The cursor that can be used in a request
     */
    CursorContext encodeCursor(Pageable pageable, UriComponents uriComponents);

    /**
     * The cursor with its context.
     * <p>
     * This object represents the pagination information as encoded in a request
     *
     * @param cursor The cursor (can be null if no cursor is present in the request)
     * @param pageSize The size of a page
     * @param sort Sorting of the resultset
     */
    @Builder
    record CursorContext(
            @Nullable
            String cursor,
            int pageSize,
            @NonNull
            Sort sort
    ) {

    }

    /**
     * Thrown when a cursor can not be decoded for any reason
     */
    @StandardException
    class CursorDecodeException extends Exception {

    }
}
