package com.contentgrid.spring.data.pagination.cursor;

import org.springframework.data.domain.Pageable;

public interface CursorEncoder {
    String encodeCursor(Pageable pageable, String referenceUrl);
}
