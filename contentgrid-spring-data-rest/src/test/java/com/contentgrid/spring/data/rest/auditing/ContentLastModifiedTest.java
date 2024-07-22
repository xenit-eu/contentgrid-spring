package com.contentgrid.spring.data.rest.auditing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.store.CustomerContentStore;
import com.contentgrid.spring.test.fixture.invoicing.store.InvoiceContentStore;
import com.contentgrid.spring.test.security.WithMockJwt;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
public class ContentLastModifiedTest {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String ORG_XENIT_VAT = "BE0887582365";

    static UUID XENIT_ID;
    static UUID INVOICE_1_ID;

    static String CUSTOMER_CONTENT_URL;
    static String INVOICE_CONTENT_URL;

    static Instant CUSTOMER_TIMESTAMP;
    static Instant INVOICE_TIMESTAMP;

    private static final String EXT_ASCII_TEXT = "L'éducation doit être gratuite.";
    private static final String MIMETYPE_PLAINTEXT_LATIN1 = "text/plain;charset=ISO-8859-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    CustomerRepository customers;

    @Autowired
    InvoiceRepository invoices;

    @Autowired
    InvoiceContentStore invoicesContent;

    @Autowired
    CustomerContentStore customersContent;

    String formatInstant(Instant date) {
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("GMT"));
        return formatter.format(date);
    }

    @BeforeEach
    void setupTestData() throws Exception {
        var xenit = customers.save(new Customer("XeniT", ORG_XENIT_VAT));

        XENIT_ID = xenit.getId();
        CUSTOMER_CONTENT_URL = "/customers/%s/content".formatted(XENIT_ID);

        INVOICE_1_ID = invoices.save(
                new Invoice(INVOICE_NUMBER_1, true, false, xenit, new HashSet<>())).getId();
        INVOICE_CONTENT_URL = "/invoices/%s/content".formatted(INVOICE_1_ID);

        var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));

        var customer = customers.findById(XENIT_ID).orElseThrow();
        customersContent.setContent(customer, PropertyPath.from("content"), stream);
        customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_LATIN1);
        customer.getContent().setFilename("content.txt");
        customers.save(customer);

        var invoice = invoices.findById(INVOICE_1_ID).orElseThrow();
        invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
        invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
        invoice.setContentFilename("content.txt");
        invoices.save(invoice);

        INVOICE_TIMESTAMP = getLastModified(INVOICE_CONTENT_URL);
        CUSTOMER_TIMESTAMP = getLastModified(CUSTOMER_CONTENT_URL);
    }

    @AfterEach
    void cleanupTestData() {
        invoices.deleteAll();
        customers.deleteAll();
    }

    private Instant getLastModified(String url) throws Exception {
        return Instant.ofEpochMilli(mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getDateHeader(HttpHeaders.LAST_MODIFIED));
    }

    @Nested
    class InlinedContentProperty {

        @Test
        void getInvoiceContent_withRecentIfModifiedSince_http304() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(get(INVOICE_CONTENT_URL)
                            .accept(MediaType.TEXT_PLAIN)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNotModified());
        }

        @Test
        void getInvoiceContent_withOutdatedIfModifiedSince_http200() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(get(INVOICE_CONTENT_URL)
                            .accept(MediaType.TEXT_PLAIN)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());
        }

        @Test
        void putInvoiceContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put(INVOICE_CONTENT_URL)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());

            assertThat(getLastModified(INVOICE_CONTENT_URL)).isAfterOrEqualTo(INVOICE_TIMESTAMP);
        }

        @Test
        void putInvoiceContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put(INVOICE_CONTENT_URL)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            assertThat(getLastModified(INVOICE_CONTENT_URL)).isEqualTo(INVOICE_TIMESTAMP);
        }

        @Test
        void deleteInvoiceContent_withRecentIfUnmodifiedSince_http204() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete(INVOICE_CONTENT_URL)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deleteInvoiceContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = INVOICE_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete(INVOICE_CONTENT_URL)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            assertThat(getLastModified(INVOICE_CONTENT_URL)).isEqualTo(INVOICE_TIMESTAMP);
        }
    }

    @Nested
    class EmbeddedContentProperty {

        @Test
        void getCustomerContent_withRecentIfModifiedSince_http304() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(get(CUSTOMER_CONTENT_URL)
                            .accept(MediaType.TEXT_PLAIN)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNotModified());
        }

        @Test
        void getCustomerContent_withOutdatedIfModifiedSince_http200() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(get(CUSTOMER_CONTENT_URL)
                            .accept(MediaType.TEXT_PLAIN)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());
        }

        @Test
        void putCustomerContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put(CUSTOMER_CONTENT_URL)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());

            assertThat(getLastModified(CUSTOMER_CONTENT_URL)).isAfterOrEqualTo(CUSTOMER_TIMESTAMP);
        }

        @Test
        void putCustomerContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put(CUSTOMER_CONTENT_URL)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            assertThat(getLastModified(CUSTOMER_CONTENT_URL)).isEqualTo(CUSTOMER_TIMESTAMP);
        }

        @Test
        void deleteCustomerContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete(CUSTOMER_CONTENT_URL)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deleteCustomerContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = CUSTOMER_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete(CUSTOMER_CONTENT_URL)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            assertThat(getLastModified(CUSTOMER_CONTENT_URL)).isEqualTo(CUSTOMER_TIMESTAMP);
        }
    }
}
