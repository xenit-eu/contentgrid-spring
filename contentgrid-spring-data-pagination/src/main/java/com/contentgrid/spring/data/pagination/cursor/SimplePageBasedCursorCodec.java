package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class SimplePageBasedCursorCodec implements CursorCodec {

    @Override
    public Pageable decodeCursor(CursorContext context) {
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(context.cursor());
        } catch (NumberFormatException ex) {
            pageNumber = 0;
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
