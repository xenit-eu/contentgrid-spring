package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface CursorCodec {

    Pageable decodeCursor(CursorContext context);

    CursorContext encodeCursor(Pageable pageable);

    record CursorContext(
            String cursor,
            int pageSize,
            Sort sort
    ) {

    }
}
