package com.contentgrid.spring.data.rest.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.PlainMapping;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Refund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = InvoicingApplication.class)
class JsonPropertyPathConverterTest {

    @Autowired
    @PlainMapping(followingAssociations = true)
    DomainTypeMapping domainTypeMapping;

    JsonPropertyPathConverter jsonPropertyPathConverter;

    @BeforeEach
    void setup() {
        jsonPropertyPathConverter = new JsonPropertyPathConverter(domainTypeMapping);
    }

    @Test
    void convertsDirectPathToJsonEquivalent_withoutAnnotation() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Invoice.class, "draft")).isEqualTo("draft");
    }

    @Test
    void convertsDirectPathToJsonEquivalent_withJsonProperty() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Invoice.class, "contentLength")).isEqualTo(
                "content_length");
    }

    @Test
    void convertsDirectPathToJsonEquivalent_withJsonIgnore() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Invoice.class, "contentId")).isNull();
    }

    @Test
    void convertsNestedPathToJsonEquivalent_withoutAnnotation() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Customer.class, "content.filename")).isEqualTo(
                "content.filename");
    }

    @Test
    void convertsNestedPathToJsonEquivalent_withJsonProperty() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Refund.class, "invoice.contentMimetype")).isEqualTo(
                "invoice.content_mimetype");
    }

    @Test
    void convertsNestedPathToJsonEquivalent_withJsonIgnore() {
        assertThat(jsonPropertyPathConverter.fromJavaPropertyPath(Customer.class, "content.id")).isNull();
    }

    @Test
    void convertNonExistingPath_throws() {
        assertThatThrownBy(() -> jsonPropertyPathConverter.fromJavaPropertyPath(Customer.class, "unknownProperty"))
                .isInstanceOf(IllegalStateException.class);
    }

}