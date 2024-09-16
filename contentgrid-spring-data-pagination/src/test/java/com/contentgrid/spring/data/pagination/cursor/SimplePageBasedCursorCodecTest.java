package com.contentgrid.spring.data.pagination.cursor;


import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorDecodeException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

class SimplePageBasedCursorCodecTest {

    CursorCodec codec = new SimplePageBasedCursorCodec();

    private static final Sort SORT = Sort.by(Order.asc("abc"), Order.desc("def"));

    @Test
    void decodeCursorFromNumber() throws CursorDecodeException {
        var pageable = codec.decodeCursor(CursorContext.builder()
                .cursor("5")
                .pageSize(15)
                .sort(SORT)
                .build());

        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getPageNumber()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(SORT);
    }

    @Test
    void decodeCursorFromNegativeNumber() {
        assertThatThrownBy(() -> {
            codec.decodeCursor(CursorContext.builder().cursor("-8").pageSize(15).sort(SORT).build());
        }).isInstanceOf(CursorDecodeException.class);
    }

    @Test
    void decodeCursorFromNonNumber() {
        assertThatThrownBy(() -> {
            codec.decodeCursor(CursorContext.builder().cursor("blabla").pageSize(15).sort(SORT).build());
        }).isInstanceOf(CursorDecodeException.class);
    }

    @Test
    void decodeCursorFromNull() throws CursorDecodeException {
        var pageable = codec.decodeCursor(CursorContext.builder().cursor(null).pageSize(15).sort(SORT).build());

        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getSort()).isEqualTo(SORT);
    }

    @Test
    void encodeCursor() {
        var cursor = codec.encodeCursor(PageRequest.of(12, 34).withSort(SORT));

        assertThat(cursor.cursor()).isEqualTo("12");
        assertThat(cursor.pageSize()).isEqualTo(34);
        assertThat(cursor.sort()).isEqualTo(SORT);
    }

}