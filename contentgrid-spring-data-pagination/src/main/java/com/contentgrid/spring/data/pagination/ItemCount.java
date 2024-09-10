package com.contentgrid.spring.data.pagination;

public record ItemCount(
        long count,
        boolean estimate
) {

}
