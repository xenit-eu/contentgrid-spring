package com.contentgrid.spring.data.pagination.jpa;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.contentgrid.spring.data.pagination.ItemCountPage;
import com.contentgrid.spring.data.pagination.jpa.strategy.JpaQuerydslItemCountStrategy;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.querydsl.core.types.dsl.Expressions;
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
    void providesExactCountForEmptySet() {
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
    void providesExactCountForPartialResultSet() {
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
    void providesEstimatedMinimumCountWhenHasNextPage() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        var result = customerRepository.findAll(Expressions.TRUE, Pageable.ofSize(10));

        assertThat(result).isInstanceOfSatisfying(ItemCountPage.class, page -> {
            assertThat(page.getTotalItemCount().estimate()).isTrue();
            // There is a next page, so there are more than 10 results
            assertThat(page.hasNext()).isTrue();
            assertThat(page.getTotalItemCount().count()).isGreaterThan(10);
        });
    }

    @Test
    void handlesUnpagedRequest() {
        Mockito.when(mockCountingStrategy.countQuery(Mockito.any()))
                .thenReturn(Optional.empty());

        var result = customerRepository.findAll(Expressions.TRUE, Pageable.unpaged());

        assertThat(result).isInstanceOfSatisfying(Page.class, page -> {
            // Unpaged query, so all results are in
            assertThat(page.hasNext()).isFalse();
            assertThat(page.getTotalElements()).isEqualTo(25);
        });
    }

}