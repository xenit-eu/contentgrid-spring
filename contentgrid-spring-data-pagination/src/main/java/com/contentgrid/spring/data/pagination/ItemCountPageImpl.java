package com.contentgrid.spring.data.pagination;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

public class ItemCountPageImpl<T> extends SliceImpl<T> implements ItemCountPage<T> {

    @Getter
    private final ItemCount totalItemCount;

    public ItemCountPageImpl(
            List<T> content,
            Pageable pageable,
            boolean hasNext,
            Supplier<Optional<ItemCount>> totalItemCount
    ) {
        super(content, pageable, hasNext);
        this.totalItemCount = adjustItemCount(calculateItemCount(totalItemCount));
    }

    private ItemCountPageImpl(
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
        return new ItemCountPageImpl<>(
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
        return totalItemCount.count();
    }

    private ItemCount adjustItemCount(ItemCount cr) {
        return getPageable().toOptional()
                .map(page -> {
                    if (hasNext()) {
                        // There has to be a next page, so adjust count to have at least one item on the next page
                        var countForItemOnNextPage = page.getOffset() + page.getPageSize() + 1;
                        return cr.orMinimally(countForItemOnNextPage);
                    } else if (hasPrevious()) {
                        // This is the last page, adjust count to amount on this page
                        return cr.orMaximally(page.getOffset() + getContent().size());
                    } else {
                        // This is the only page; we know exactly the count on this page
                        return ItemCount.exact(getContent().size());
                    }
                })
                .orElse(cr);
    }

    private ItemCount calculateItemCount(Supplier<Optional<ItemCount>> supplier) {
        return staticallyDerivedItemCount()
                .or(supplier)
                .orElseGet(ItemCount::unknown);
    }

    private Optional<ItemCount> staticallyDerivedItemCount() {
        if (hasNext()) {
            // There is a next page, have the actual count result be resolved
            return Optional.empty();
        }
        if (!hasContent() && hasPrevious()) {
            // The resultset is empty; we don't know if there will be things on the page directly before this one
            return Optional.empty();
        }

        // If this is exactly the last page with results: we know the exact size, no need for counting
        return Optional.of(ItemCount.exact(getPageable().getOffset() + getContent().size()));
    }
}
