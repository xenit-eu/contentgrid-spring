package com.contentgrid.spring.data.querydsl.sort;

import com.querydsl.core.types.OrderSpecifier;
import java.util.List;
import lombok.experimental.Delegate;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;

public class QSortWithOriginalSort extends QSort {
    @Delegate
    private final Sort originalSort;

    public QSortWithOriginalSort(Sort originalSort, OrderSpecifier<?>... orderSpecifiers) {
        super(orderSpecifiers);
        this.originalSort = originalSort;
    }

    public QSortWithOriginalSort(Sort originalSort, List<OrderSpecifier<?>> orderSpecifiers) {
        super(orderSpecifiers);
        this.originalSort = originalSort;
    }

}
