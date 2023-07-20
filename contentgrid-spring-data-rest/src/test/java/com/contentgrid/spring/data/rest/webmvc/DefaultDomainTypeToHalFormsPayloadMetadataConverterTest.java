package com.contentgrid.spring.data.rest.webmvc;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
        ContentGridSpringDataRestProfileConfiguration.class
})
class DefaultDomainTypeToHalFormsPayloadMetadataConverterTest {

    @Autowired
    DefaultDomainTypeToHalFormsPayloadMetadataConverter converter;

    @Test
    void convertToCreatePayloadMetadata_embeddedContent() {
        var metadata = converter.convertToCreatePayloadMetadata(Customer.class);

        assertThat(metadata.getType()).isEqualTo(Customer.class);

        assertThat(metadata.stream()).satisfiesExactlyInAnyOrder(
                name -> {
                    assertThat(name.getName()).isEqualTo("name");
                    assertThat(name.isReadOnly()).isFalse();
                    assertThat(name.isRequired()).isFalse();
                },
                vat -> {
                    assertThat(vat.getName()).isEqualTo("vat");
                    assertThat(vat.isReadOnly()).isFalse();
                    assertThat(vat.isRequired()).isTrue();
                },
                contentMimetype -> {
                    assertThat(contentMimetype.getName()).isEqualTo("content.mimetype");
                    assertThat(contentMimetype.isReadOnly()).isFalse();
                    assertThat(contentMimetype.isRequired()).isFalse();
                },
                contentFilename -> {
                    assertThat(contentFilename.getName()).isEqualTo("content.filename");
                    assertThat(contentFilename.isReadOnly()).isFalse();
                    assertThat(contentFilename.isRequired()).isFalse();
                },
                orders -> {
                    assertThat(orders.getName()).isEqualTo("orders");
                },
                invoices -> {
                    assertThat(invoices.getName()).isEqualTo("invoices");
                }
        );
    }

    @Test
    void convertToCreatePayloadMetadata_association() {
        var metadata = converter.convertToCreatePayloadMetadata(Order.class);

        assertThat(metadata.getType()).isEqualTo(Order.class);

        assertThat(metadata.stream()).satisfiesExactlyInAnyOrder(
                customer -> {
                    assertThat(customer.getName()).isEqualTo("customer");
                    assertThat(customer.isReadOnly()).isFalse();
                    assertThat(customer.isRequired()).isFalse();
                },
                invoice -> {
                    assertThat(invoice.getName()).isEqualTo("invoice");
                    assertThat(invoice.isReadOnly()).isFalse();
                    assertThat(invoice.isRequired()).isFalse();
                },
                shippingAddress -> {
                    assertThat(shippingAddress.getName()).isEqualTo("shipping_address");
                    assertThat(shippingAddress.isReadOnly()).isFalse();
                    assertThat(shippingAddress.isRequired()).isFalse();
                },
                promos -> {
                    assertThat(promos.getName()).isEqualTo("promos");
                }
        );
    }

    @Test
    void convertToCreatePayloadMetadata_requiredAssociation() {
        var metadata = converter.convertToCreatePayloadMetadata(Invoice.class);

        assertThat(metadata.stream()).anySatisfy(
                counterparty -> {
                    assertThat(counterparty.getName()).isEqualTo("counterparty");
                    assertThat(counterparty.isRequired()).isTrue();
                }
        );
    }

    @Test
    void convertToSearchPayloadMetadata_embeddedContent() {
        var metadata = converter.convertToSearchPayloadMetadata(Customer.class);

        assertThat(metadata.stream()).satisfiesExactlyInAnyOrder(
                vat -> {
                    assertThat(vat.getName()).isEqualTo("vat");
                },
                contentSize -> {
                    assertThat(contentSize.getName()).isEqualTo("content.size");
                },
                contentMimetype -> {
                    assertThat(contentMimetype.getName()).isEqualTo("content.mimetype");
                },
                contentFilename -> {
                    assertThat(contentFilename.getName()).isEqualTo("content.filename");
                },
                invoicesNumber -> {
                    assertThat(invoicesNumber.getName()).isEqualTo("invoices.number");
                },
                invoicesNumber -> {
                    assertThat(invoicesNumber.getName()).isEqualTo("invoices.paid");
                }
        );
    }

    @Test
    void convertToSearchPayloadMetadata_toOneAssociation() {
        var metadata = converter.convertToSearchPayloadMetadata(Order.class);

        assertThat(metadata.stream()).map(PropertyMetadata::getName).containsExactlyInAnyOrder(
                "customer.vat",
                "customer.content.size",
                "customer.content.mimetype",
                "customer.content.filename",
                "invoice.number",
                "invoice.paid",
                "shipping_address.zip"
        );
    }

    @Test
    void convertToSearchPayloadMetadata_requiredAssociation() {
        var metadata = converter.convertToSearchPayloadMetadata(Invoice.class);

        assertThat(metadata.stream()).map(PropertyMetadata::getName).containsExactlyInAnyOrder(
                "number",
                "paid"
        );
    }
}
