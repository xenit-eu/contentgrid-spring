package com.contentgrid.spring.data.rest.webmvc;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(properties = { "contentgrid.rest.use-multipart-hal-forms=true" })
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
class DefaultDomainTypeToHalFormsPayloadMetadataConverterTest {

    @Autowired
    DomainTypeToHalFormsPayloadMetadataConverter converter;

    @Test
    void convertToCreatePayloadMetadata_embeddedContent() {
        var metadata = converter.convertToCreatePayloadMetadata(Customer.class).payloadMetadata();

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
                content -> {
                    assertThat(content.getName()).isEqualTo("content");
                    assertThat(content.isReadOnly()).isFalse();
                    assertThat(content.isRequired()).isFalse();
                },
                birthday -> {
                    assertThat(birthday.getName()).isEqualTo("birthday");
                    assertThat(birthday.isReadOnly()).isFalse();
                    assertThat(birthday.isRequired()).isFalse();
                },
                gender -> {
                    assertThat(gender.getName()).isEqualTo("gender");
                    assertThat(gender.isReadOnly()).isFalse();
                    assertThat(gender.isRequired()).isFalse();
                },
                totalSpend -> {
                    assertThat(totalSpend.getName()).isEqualTo("total_spend");
                    assertThat(totalSpend.isReadOnly()).isFalse();
                    assertThat(totalSpend.isRequired()).isFalse();
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
        var metadata = converter.convertToCreatePayloadMetadata(Order.class).payloadMetadata();

        assertThat(metadata.getType()).isEqualTo(Order.class);

        assertThat(metadata.stream()).satisfiesExactlyInAnyOrder(
                customer -> {
                    assertThat(customer.getName()).isEqualTo("customer");
                    assertThat(customer.isReadOnly()).isFalse();
                    assertThat(customer.isRequired()).isFalse();
                },
                shippingAddress -> {
                    assertThat(shippingAddress.getName()).isEqualTo("shipping_address");
                    assertThat(shippingAddress.isReadOnly()).isFalse();
                    assertThat(shippingAddress.isRequired()).isFalse();
                },
                promos -> {
                    assertThat(promos.getName()).isEqualTo("promos");
                },
                manualPromos -> {
                    assertThat(manualPromos.getName()).isEqualTo("manualPromos");
                }
        );
    }

    @Test
    void convertToCreatePayloadMetadata_requiredAssociation() {
        var metadata = converter.convertToCreatePayloadMetadata(Invoice.class).payloadMetadata();

        assertThat(metadata.stream()).anySatisfy(
                counterparty -> {
                    assertThat(counterparty.getName()).isEqualTo("counterparty");
                    assertThat(counterparty.isRequired()).isTrue();
                }
        );
    }

    @Test
    void convertToCreatePayloadMetadata_nonExportedAssociation() {
        var metadata = converter.convertToCreatePayloadMetadata(PromotionCampaign.class).payloadMetadata();

        assertThat(metadata.stream()).satisfiesExactlyInAnyOrder(
                promoCode -> {
                    assertThat(promoCode.getName()).isEqualTo("promoCode");
                    assertThat(promoCode.isRequired()).isTrue();
                },
                description -> {
                    assertThat(description.getName()).isEqualTo("description");
                    assertThat(description.isRequired()).isFalse();
                }
        );
    }

    @Test
    void convertToSearchPayloadMetadata_embeddedContent() {
        var metadata = converter.convertToSearchPayloadMetadata(Customer.class);

        assertThat(metadata.stream()).map(PropertyMetadata::getName).containsExactlyInAnyOrder(
                "vat",
                "birthday",
                "content.size",
                "content.mimetype",
                "content.filename",
                "invoices.number",
                "invoices.paid",
                "invoices.content.length",
                        /*"invoices.content.length.lt",
                        "invoices.content.length.gt"*/
                "invoices.orders.id"
        );
    }

    @Test
    void convertToSearchPayloadMetadata_toOneAssociation() {
        var metadata = converter.convertToSearchPayloadMetadata(Order.class);

        assertThat(metadata.stream()).map(PropertyMetadata::getName).containsExactlyInAnyOrder(
                "customer.vat",
                "customer.birthday",
                "customer.content.size",
                "customer.content.mimetype",
                "customer.content.filename",
                "shipping_address.zip"
        );
    }

    @Test
    void convertToSearchPayloadMetadata_requiredAssociation() {
        var metadata = converter.convertToSearchPayloadMetadata(Invoice.class);

        assertThat(metadata.stream()).map(PropertyMetadata::getName).containsExactlyInAnyOrder(
                "number",
                "paid",
                "content.length",
                /*"content.length.lt",
                "content.length.gt"*/
                "orders.id"
        );
    }
}
