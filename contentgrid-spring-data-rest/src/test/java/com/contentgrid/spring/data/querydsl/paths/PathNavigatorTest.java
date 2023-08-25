package com.contentgrid.spring.data.querydsl.paths;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.model.QInvoice;
import com.contentgrid.spring.test.fixture.invoicing.model.QOrder;
import com.contentgrid.spring.test.fixture.invoicing.model.QShippingAddress;
import com.querydsl.core.types.dsl.PathInits;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PathNavigatorTest {

    public static final PathNavigator CUSTOMER_NAVIGATOR = new PathNavigator(QCustomer.customer);
    public static final PathNavigator INVOICE_NAVIGATOR = new PathNavigator(QInvoice.invoice);
    public static final PathNavigator ORDER_NAVIGATOR = new PathNavigator(QOrder.order);

    @Test
    void navigateToDirectProperty() {
        assertThat(CUSTOMER_NAVIGATOR.get("id").getPath()).isEqualTo(QCustomer.customer.id);
        assertThat(INVOICE_NAVIGATOR.get("id").getPath()).isEqualTo(QInvoice.invoice.id);

        assertThat(CUSTOMER_NAVIGATOR.get("invoices").getPath()).isEqualTo(QCustomer.customer.invoices);
        assertThat(INVOICE_NAVIGATOR.get("counterparty").getPath()).isEqualTo(QInvoice.invoice.counterparty);

        assertThat(CUSTOMER_NAVIGATOR.get("content").getPath()).isEqualTo(QCustomer.customer.content);
    }

    @Test
    void navigateThroughEmbeddedProperty() {
        assertThat(CUSTOMER_NAVIGATOR.get("content").get("length").getPath()).isEqualTo(QCustomer.customer.content.length);
    }

    @Test
    void navigateThroughRelation() {
        assertThat(CUSTOMER_NAVIGATOR.get("invoices").get("number").getPath()).isEqualTo(QCustomer.customer.invoices.any().number);
        assertThat(INVOICE_NAVIGATOR.get("counterparty").get("vat").getPath()).isEqualTo(QInvoice.invoice.counterparty.vat);

        assertThat(INVOICE_NAVIGATOR.get("counterparty").get("invoices").get("number").getPath()).isEqualTo(QInvoice.invoice.counterparty.invoices.any().number);
    }

    @Test
    void navigateToDeepProperty() {
        assertThat(ORDER_NAVIGATOR.get("invoice").get("counterparty").get("content").get("filename").getPath()).isEqualTo(
                // This is so we don't run into a PathInits problem ourselves here when reading the property
                new QOrder(QOrder.order.getMetadata(), new PathInits("*.*.*")).invoice.counterparty.content.filename
        );

        assertThat(new PathNavigator(QShippingAddress.shippingAddress).get("order").get("invoice").get("counterparty").get("content").get("filename").getPath()).isEqualTo(
                new QShippingAddress(QShippingAddress.shippingAddress.getMetadata(), new PathInits("*.*.*.*"))
                        .order.invoice.counterparty.content.filename
        );

        assertThat(new PathNavigator(QShippingAddress.shippingAddress).get("order").get("invoice").get("counterparty").get("vat").getPath()).isEqualTo(
                new QShippingAddress(QShippingAddress.shippingAddress.getMetadata(), new PathInits("*.*.*"))
                        .order.invoice.counterparty.vat
        );
    }

    @Entity
    public static class TestEntityWithSameNameAsProperty {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @CollectionFilterParam
        private UUID id;

        @OneToOne
        private TestEntityWithSameNameAsProperty testEntityWithSameNameAsProperty;
    }

    @Test
    void navigateToEntityWithSameNameAsProperty() {
        var navigator = new PathNavigator(QPathNavigatorTest_TestEntityWithSameNameAsProperty.testEntityWithSameNameAsProperty1);

        assertThat(navigator.get("testEntityWithSameNameAsProperty").getPath())
                .isEqualTo(QPathNavigatorTest_TestEntityWithSameNameAsProperty.testEntityWithSameNameAsProperty1.testEntityWithSameNameAsProperty);
    }

    @Entity
    public static class Case {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @CollectionFilterParam
        private UUID id;

        @OneToOne
        private Case _case;
    }

    @Test
    void navigateToEntityWithReservedPropertyName() {
        var navigator = new PathNavigator(QPathNavigatorTest_Case.case$);

        assertThat(navigator.get("_case").getPath())
                .isEqualTo(QPathNavigatorTest_Case.case$._case);
    }

}
