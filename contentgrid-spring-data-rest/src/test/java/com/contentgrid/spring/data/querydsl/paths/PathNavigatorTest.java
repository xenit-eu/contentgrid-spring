package com.contentgrid.spring.data.querydsl.paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.model.QInvoice;
import org.junit.jupiter.api.Test;

class PathNavigatorTest {

    public static final PathNavigator CUSTOMER_NAVIGATOR = new PathNavigator(QCustomer.customer);
    public static final PathNavigator INVOICE_NAVIGATOR = new PathNavigator(QInvoice.invoice);

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

}