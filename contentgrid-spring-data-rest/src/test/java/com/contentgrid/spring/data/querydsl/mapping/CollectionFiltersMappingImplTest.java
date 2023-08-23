package com.contentgrid.spring.data.querydsl.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
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
                        "invoices.number",
                        "invoices.paid",
                        "invoices.content.length"
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
    @Disabled("TODO")
    void forIdProperty() {
        assertThat(collectionFiltersMapping.forIdProperty(Customer.class, "invoices")).hasValueSatisfying(filter -> {
            assertThat(filter.getPath()).isEqualTo(QCustomer.customer.invoices.any().id);
        });
    }

}
