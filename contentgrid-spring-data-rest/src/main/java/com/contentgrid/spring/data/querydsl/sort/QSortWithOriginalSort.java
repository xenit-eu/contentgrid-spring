package com.contentgrid.spring.data.querydsl.sort;

import com.querydsl.core.types.OrderSpecifier;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;

/**
 * This class allows using QueryDSL {@link OrderSpecifier} while keeping a reference to the original {@link Sort.Order}
 * parameters from {@link Sort}
 * <p>
 * When the <pre>sort</pre> query parameter is reconstructed based on {@linkplain Sort}, the original
 * {@linkplain Sort.Order} objects should be used instead of the ones derived from {@linkplain OrderSpecifier} (as
 * happens in {@linkplain QSort}).
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class QSortWithOriginalSort extends QSort {

    private final Sort originalSort;

    public QSortWithOriginalSort(Sort originalSort, List<OrderSpecifier<?>> orderSpecifiers) {
        super(orderSpecifiers);
        this.originalSort = originalSort;
    }

}
