package com.contentgrid.spring.data.pagination.cursor;

import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.StandardException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

public interface CursorCodec {

    Pageable decodeCursor(CursorContext context) throws CursorDecodeException;

    CursorContext encodeCursor(Pageable pageable);

    @Builder
    record CursorContext(
            @Nullable
            String cursor,
            int pageSize,
            @NonNull
            Sort sort
    ) {

    }

    @StandardException
    class CursorDecodeException extends Exception {

    }
}
