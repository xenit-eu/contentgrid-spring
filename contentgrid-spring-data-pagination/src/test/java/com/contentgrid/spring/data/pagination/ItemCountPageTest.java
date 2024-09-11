package com.contentgrid.spring.data.pagination;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ItemCountPageTest {

    private static ItemCountPage<Integer> createPage(Pageable pageable, int actualItems,
            ItemCount ItemCount) {
        var data = IntStream.rangeClosed(1, actualItems)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .boxed()
                .toList();
        return new ItemCountPage<>(
                data,
                pageable,
                actualItems > pageable.getOffset() + pageable.getPageSize(),
                () -> Optional.ofNullable(ItemCount)
        );
    }

    @Nested
    class WithoutCounts {

        @Test
        void noResults_exactCount() {
            var page = createPage(PageRequest.ofSize(10), 0, null);

            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(0));
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        void onePage_exactCount() {
            var page = createPage(PageRequest.ofSize(10), 6, null);

            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(6));
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isFalse();

        }

        @Test
        void multiplePages_estimateCount() {
            var page = createPage(PageRequest.ofSize(10), 20, null);

            // Best estimate: at least one more than the current page
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(11));
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        void multiplePages_empty() {
            var page = createPage(PageRequest.ofSize(10).withPage(5), 20, null);

            // We can't really estimate at all; our current page is empty, and we don't know if there is something on the previous page
            // so we make a wild guess that there might be data on the previous page
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(50));
            assertThat(page.hasPrevious()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }

    }

    @Nested
    class EstimatesTooSmall {

        @Test
        void hasNextPage_adjustedCount() {
            var page = createPage(PageRequest.ofSize(10), 20, ItemCount.estimated(5));

            // There is a next page, so there are more than 10 results. We don't know exactly how many
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(11));
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isFalse();
        }

        @Test
        void noNextPage_adjustedCount() {
            var page = createPage(PageRequest.ofSize(10).withPage(1), 20, ItemCount.estimated(5));

            // There is no next page, so we know that this page has the final number of results
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(20));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isTrue();
        }

        @Test
        void hasNextPage_countJustTooSmall() {
            var page = createPage(PageRequest.ofSize(10), 11, ItemCount.estimated(10));

            // There is a next page, so we know that there must be more than 10 results. We don't know exactly how many
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(11));
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isFalse();
        }

        @Test
        void multiplePages_empty() {
            var page = createPage(PageRequest.ofSize(10).withPage(5), 20, ItemCount.estimated(5));

            // Our current page is empty, and we don't know if there is something on the previous page,
            // so we take the provided estimate
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(5));
            assertThat(page.hasPrevious()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }
    }

    @Nested
    class EstimatesTooLarge {

        @Test
        void noNextPage_adjustedCount() {
            var page = createPage(PageRequest.ofSize(10).withPage(1), 18, ItemCount.estimated(50));

            // There is no next page, so we know that this page has the final number of results
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(18));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isTrue();
        }

        @Test
        void noNextPage_countJustTooLarge() {
            var page = createPage(PageRequest.ofSize(10), 10, ItemCount.estimated(11));

            // There is no next page, so we know that this page has the final number of results
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(10));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isFalse();
        }

        @Test
        void multiplePages_empty() {
            var page = createPage(PageRequest.ofSize(10).withPage(5), 20, ItemCount.estimated(500));

            // Our current page is empty, but we know we are not yet at result 500, so we can estimate our results down
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(50));
            assertThat(page.hasPrevious()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }
    }

    @Test
    void noNextPage_correctCount() {
        var page = createPage(PageRequest.ofSize(10), 10, ItemCount.estimated(10));

        // There is no next page, so we know that this page has the final number of results
        assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(10));
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void hasNextPage_correctCount() {
        var page = createPage(PageRequest.ofSize(10), 12, ItemCount.estimated(12));

        assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(12));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void mapping_retainsCountInformation() {
        var page = createPage(PageRequest.ofSize(10), 20, ItemCount.estimated(150));
        var mappedPage = page.map(i -> i * 2);

        assertThat(mappedPage.getTotalItemCount()).isEqualTo(page.getTotalItemCount());
        assertThat(mappedPage).containsExactly(2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
    }

    @Test
    void totalElements_derivedFromItemCount() {
        var page = createPage(PageRequest.ofSize(10), 152, ItemCount.estimated(152));

        assertThat(page.getTotalElements()).isEqualTo(152);
        assertThat(page.getTotalPages()).isEqualTo(16);
    }
}