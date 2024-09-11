package com.contentgrid.spring.data.pagination;

public record ItemCount(
        long count,
        boolean estimate
) {

    public static ItemCount exact(long count) {
        return new ItemCount(count, false);
    }

    public static ItemCount estimated(long estimate) {
        return new ItemCount(estimate, true);
    }
}
