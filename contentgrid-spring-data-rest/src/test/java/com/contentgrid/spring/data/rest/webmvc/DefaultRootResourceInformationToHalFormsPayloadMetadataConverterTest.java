package com.contentgrid.spring.data.rest.webmvc;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = InvoicingApplication.class)
class DefaultRootResourceInformationToHalFormsPayloadMetadataConverterTest {

    @Autowired
    Repositories repositories;
    @Autowired
    RepositoryInvokerFactory invokerFactory;
    @Autowired
    ResourceMappings mappings;

    protected RootResourceInformation getResourceInformation(Class<?> domainType) {

        PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainType);

        return new RootResourceInformation(mappings.getMetadataFor(domainType), entity,
                invokerFactory.getInvokerFor(domainType));
    }

    @Test
    void convertToCreatePayloadMetadata_embeddedContent() {
        var converter = new DefaultRootResourceInformationToHalFormsPayloadMetadataConverter();

        var metadata = converter.convertToCreatePayloadMetadata(getResourceInformation(Customer.class));

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
        var converter = new DefaultRootResourceInformationToHalFormsPayloadMetadataConverter();

        var metadata = converter.convertToCreatePayloadMetadata(getResourceInformation(Order.class));

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
        var converter = new DefaultRootResourceInformationToHalFormsPayloadMetadataConverter();

        var metadata = converter.convertToCreatePayloadMetadata(getResourceInformation(Invoice.class));

        assertThat(metadata.stream()).anySatisfy(
                counterparty -> {
                    assertThat(counterparty.getName()).isEqualTo("counterparty");
                    assertThat(counterparty.isRequired()).isTrue();
                }
        );
    }
}