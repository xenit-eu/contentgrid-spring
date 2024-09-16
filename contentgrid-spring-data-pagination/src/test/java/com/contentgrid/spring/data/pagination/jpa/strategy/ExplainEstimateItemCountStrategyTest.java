package com.contentgrid.spring.data.pagination.jpa.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = InvoicingApplication.class)
class ExplainEstimateItemCountStrategyTest {

    @Autowired
    ExplainEstimateItemCountStrategy explainEstimateCountingStrategy;

    @Autowired
    EntityManager entityManager;

    @Autowired
    CustomerRepository customerRepository;

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
    // Test needs to run in a transaction to have an open entity manager to perform queries
    @Transactional
    void performEstimates() {
        Supplier<JPQLQuery<?>> querySupplier = () -> new JPAQuery<>(entityManager)
                .from(QCustomer.customer)
                .select(QCustomer.customer);

        assertThat(explainEstimateCountingStrategy.countQuery(querySupplier))
                .hasValueSatisfying(result -> {
                    assertThat(result.isEstimated()).isTrue();
                    assertThat(result.count())
                            .isGreaterThanOrEqualTo(1)
                            // Expecting that the postgres estimates are not that much off for a small table
                            .isLessThan(1_000_000);
                });
    }

}