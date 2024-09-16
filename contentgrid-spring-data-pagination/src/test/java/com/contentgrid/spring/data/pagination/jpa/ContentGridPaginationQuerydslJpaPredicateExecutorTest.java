package com.contentgrid.spring.data.pagination.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.contentgrid.spring.data.pagination.ItemCountPage;
import com.contentgrid.spring.data.pagination.jpa.strategy.JpaQuerydslItemCountStrategy;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@SpringBootTest(classes = InvoicingApplication.class)
class ContentGridPaginationQuerydslJpaPredicateExecutorTest {

    @Autowired
    CustomerRepository customerRepository;

    @MockBean
    JpaQuerydslItemCountStrategy mockCountingStrategy;

    @BeforeEach
    void seedCustomers() {
        customerRepository.saveAllAndFlush(
                IntStream.range(0, 25)
                        .mapToObj(number -> {
                            var customer = new Customer();
                            customer.setName("Customer %d".formatted(number));
                            customer.setVat("VAT%d".formatted(number));
                            return customer;
                        })
                        .toList()
        );
    }

    @AfterEach
    void destroyCustomers() {
        customerRepository.deleteAll();
    }

    @Test
    void noResults() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        var result = customerRepository.findAll(QCustomer.customer.name.isEmpty(), Pageable.ofSize(10));

        assertThat(result).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(0));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isFalse();
        });
    }

    @Test
    void resultsOnSinglePage() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        // Matches items 20 to 24 and item 2
        var result = customerRepository.findAll(QCustomer.customer.vat.startsWith("VAT2"), Pageable.ofSize(10));

        assertThat(result).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(6));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isFalse();
        });
    }

    @Test
    void resultsOnNextPage() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        Page<Customer> firstPage = customerRepository.findAll(QCustomer.customer.vat.startsWith("VAT"),
                Pageable.ofSize(10));
        Page<Customer> nextPage = customerRepository.findAll(QCustomer.customer.vat.startsWith("VAT"),
                firstPage.nextPageable());
        Page<Customer> lastPage = customerRepository.findAll(QCustomer.customer.vat.startsWith("VAT"),
                nextPage.nextPageable());

        assertThat(firstPage).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isFalse();
            // Count is estimated (there is a next page, so at least 11 results)
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(11));
        });

        assertThat(nextPage).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isTrue();
            // Count is estimated (there is a next page, so at least 21 results)
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(21));
        });

        assertThat(lastPage).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isTrue();
            // Count is exactly known (there is no next page; 2 pages before us, and there are a couple of results on this page)
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.exact(25));
        });
    }

    @Test
    void resultsOnEmptyPage() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        // Going to an arbitrary page somewhere far away, where there is no data anymore
        Page<Customer> result = customerRepository.findAll(QCustomer.customer.vat.startsWith("VAT"),
                Pageable.ofSize(10).withPage(8));
        assertThat(result).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.getTotalItemCount()).isEqualTo(ItemCount.estimated(80));
            assertThat(page.hasNext()).isFalse();
            assertThat(page.hasPrevious()).isTrue();
        });
    }

}
