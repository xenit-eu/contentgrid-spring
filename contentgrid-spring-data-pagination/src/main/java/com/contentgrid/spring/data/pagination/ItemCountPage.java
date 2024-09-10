package com.contentgrid.spring.data.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

public class ItemCountPage<T> extends SliceImpl<T> implements Page<T> {

    private final ItemCount totalItemCount;

    public ItemCountPage(
            List<T> content,
            Pageable pageable,
            boolean hasNext,
            ItemCount totalItemCount
    ) {
        super(content, pageable, hasNext);
        this.totalItemCount = totalItemCount;
    }

    @Override
    public <U> ItemCountPage<U> map(Function<? super T, ? extends U> converter) {
        return new ItemCountPage<>(
                getConvertedContent(converter),
                getPageable(),
                hasNext(),
                totalItemCount
        );
    }

    @Override
    public int getTotalPages() {
        if (getSize() == 0) {
            return 1;
        }
        return (int) Math.ceil((double) getTotalElements() / getSize());
    }

    @Override
    public long getTotalElements() {
        var totalResult = totalItemCount.count();

        return getPageable().toOptional()
                .map(page -> {
                    var countForItemOnNextPage = page.getOffset() + page.getPageSize() + 1;
                    if (hasNext()) {
                        // There has to be a next page, so adjust count to have at least one item on the next page
                        return Math.max(totalResult, countForItemOnNextPage);
                    } else {
                        // This is the last page, adjust count to amount on this page
                        return page.getOffset() + getContent().size();
                    }
                })
                .orElse(totalResult);
    }

    @Override
    public Pageable nextOrLastPageable() {
        return super.nextOrLastPageable();
    }

    @Override
    public Pageable previousOrFirstPageable() {
        return super.previousOrFirstPageable();
    }
}
