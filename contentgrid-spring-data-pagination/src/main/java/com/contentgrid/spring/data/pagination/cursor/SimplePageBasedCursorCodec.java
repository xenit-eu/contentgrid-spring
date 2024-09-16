package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class SimplePageBasedCursorCodec implements CursorCodec {

    @Override
    public Pageable decodeCursor(CursorContext context) throws CursorDecodeException {
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(context.cursor());
            if (pageNumber < 0) {
                throw new CursorDecodeException("may not be negative");
            }
        } catch (NumberFormatException ex) {
            throw new CursorDecodeException("must be a number", ex);
        }
        return PageRequest.of(pageNumber, context.pageSize(), context.sort());
    }

    @Override
    public CursorContext encodeCursor(Pageable pageable) {
        return new CursorContext(
                Integer.toString(pageable.getPageNumber()),
                pageable.getPageSize(), pageable.getSort()
        );
    }

}
