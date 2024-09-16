package com.contentgrid.spring.data.pagination.jpa.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

@SpringBootTest(
        classes = InvoicingApplication.class,
        properties = {
                "spring.datasource.hikari.connection-init-sql=set search_path=intercept,public"
        })
class TimedDirectCountItemCountStrategyTest {

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
    void destroyCustomers() throws SQLException {
        // Ensure that interception schema is cleaned up.
        // This MUST run outside of a transaction used for tests, because otherwise the query blocks
        // because the open transaction has objects in this schema in use
        try (var conn = dataSource.getConnection()) {
            try (var statement = conn.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS intercept CASCADE;");
            }
        }

        // Clean up created customer objects
        customerRepository.deleteAll();
    }

    @Autowired
    TimedDirectCountItemCountStrategy timedDirectCountCountingStrategy;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void performCount_withinTime() {
        // Test needs to run in a transaction to have an open entity manager to perform queries
        var tx = transactionManager.getTransaction(TransactionDefinition.withDefaults());
        try {
            Supplier<JPQLQuery<?>> querySupplier = () -> new JPAQuery<>(entityManager)
                    .from(QCustomer.customer)
                    .select(QCustomer.customer);

            assertThat(timedDirectCountCountingStrategy.countQuery(querySupplier))
                    .hasValueSatisfying(result -> {
                        assertThat(result.isEstimated()).isFalse();
                        assertThat(result.count()).isEqualTo(25);
                    });
        } finally {
            transactionManager.commit(tx);
        }
    }

    @Test
    void performCount_outOfTime() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            try (var statement = conn.createStatement()) {
                statement.addBatch("CREATE SCHEMA intercept;");
                statement.addBatch("""
                        CREATE VIEW intercept.customer AS
                            SELECT customer.*
                            FROM public.customer
                            CROSS JOIN LATERAL pg_sleep(2);
                        """);

                statement.executeBatch();
            }
        }

        // Test needs to run in a transaction to have an open entity manager to perform queries
        var tx = transactionManager.getTransaction(TransactionDefinition.withDefaults());
        try {
            Supplier<JPQLQuery<?>> querySupplier = () -> new JPAQuery<>(entityManager)
                    .from(QCustomer.customer)
                    .select(QCustomer.customer);

            // Counting timed out (due to our delay function), no result is expected
            assertThat(timedDirectCountCountingStrategy.countQuery(querySupplier)).isEmpty();
        } finally {
            transactionManager.commit(tx);
        }

    }

}