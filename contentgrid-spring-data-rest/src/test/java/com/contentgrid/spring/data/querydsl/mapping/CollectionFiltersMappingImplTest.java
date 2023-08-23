package com.contentgrid.spring.data.querydsl.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.model.QInvoice;
import com.contentgrid.spring.test.fixture.invoicing.model.QOrder;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class
})
class CollectionFiltersMappingImplTest {
    @Autowired
    CollectionFiltersMapping collectionFiltersMapping;

    @Test
    void forDomainType() {
        assertThat(collectionFiltersMapping.forDomainType(Customer.class).filters())
                .map(CollectionFilter::getFilterName)
                .containsExactlyInAnyOrder(
                        "vat",
                        "content.size",
                        "content.mimetype",
                        "content.filename",
                        "invoices.$$id",
                        "invoices.number",
                        "invoices.paid",
                        "invoices.content.length",
                        "invoices.orders.id",
                        "invoices.counterparty",
                        "orders"
                        // not included because predicate is None
                        // "invoices.content.length.lt",
                        // "invoices.content.length.gt"
                );
    }

    @Test
    void forProperty() {
        assertThat(collectionFiltersMapping.forProperty(Customer.class, "vat"))
                .hasValueSatisfying(filter -> {
                    assertThat(filter.getFilterName()).isEqualTo("vat");
                    assertThat(filter.getPath()).isEqualTo(QCustomer.customer.vat);
                    assertThat(filter.isDocumented()).isTrue();
                });

        assertThat(collectionFiltersMapping.forProperty(Customer.class, "content", "length"))
                .hasValueSatisfying(filter -> {
                    assertThat(filter.getFilterName()).isEqualTo("content.size");
                    assertThat(filter.getPath()).isEqualTo(QCustomer.customer.content.length);
                    assertThat(filter.isDocumented()).isTrue();
                });

        assertThat(collectionFiltersMapping.forProperty(Customer.class, "content")).isEmpty();

        assertThatThrownBy(() -> collectionFiltersMapping.forProperty(Customer.class, "content", "size"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path 'customer.content' does not have property 'size'");

    }

    @Test
    void forIdProperty() {
        assertThat(collectionFiltersMapping.forIdProperty(Customer.class, "invoices")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("invoices.$$id");
            assertThat(filter.getPath()).isEqualTo(QCustomer.customer.invoices.any().id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Order.class, "invoice")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("invoice._id");
            assertThat(filter.getPath()).isEqualTo(QOrder.order.invoice.id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Invoice.class, "counterparty")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("counterparty");
            assertThat(filter.getPath()).isEqualTo(QInvoice.invoice.counterparty.id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Invoice.class, "orders")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("orders.id");
            assertThat(filter.getPath()).isEqualTo(QInvoice.invoice.orders.any().id);
            assertThat(filter.isDocumented()).isTrue();
        });

        assertThat(collectionFiltersMapping.forIdProperty(ShippingAddress.class, "order")).isEmpty();
    }

}
