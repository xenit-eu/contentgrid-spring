package com.contentgrid.spring.data.rest.etag;

import static com.contentgrid.spring.test.matchers.ETagHeaderMatcher.toETag;
import static com.contentgrid.spring.test.matchers.ExtendedHeaderResultMatchers.headers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
public class OptimisticLockingTest {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String ORG_XENIT_VAT = "BE0887582365";
    static UUID INVOICE_1_ID;
    static UUID XENIT_ID;

    static int INVOICE_1_VERSION;
    static int XENIT_VERSION;
    static final String INVALID_VERSION = "\"INVALID\"";

    private static final String EXT_ASCII_TEXT = "L'éducation doit être gratuite.";
    private static final String UNICODE_TEXT = "Some unicode text 💩";
    private static final String MIMETYPE_PLAINTEXT_LATIN1 = "text/plain;charset=ISO-8859-1";
    private static final String MIMETYPE_PLAINTEXT_UTF8 = "text/plain;charset=UTF-8";

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

    @BeforeEach
    void setupTestData() {
        var xenit = customers.save(
                new Customer(null, 0, "XeniT", ORG_XENIT_VAT, null, null, null, new HashSet<>(),
                        new HashSet<>()));

        XENIT_ID = xenit.getId();
        XENIT_VERSION = xenit.getVersion();

        var invoice = invoices.save(
                new Invoice(INVOICE_NUMBER_1, true, false, xenit, new HashSet<>()));

        INVOICE_1_ID = invoice.getId();
        INVOICE_1_VERSION = invoice.getVersion();
    }

    void setupContentProperties() {
        var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));

        var customer = customers.findById(XENIT_ID).orElseThrow();
        customersContent.setContent(customer, PropertyPath.from("content"), stream);
        customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_LATIN1);
        customer.getContent().setFilename("content.txt");
        customer = customers.save(customer);

        XENIT_VERSION = customer.getVersion();

        var invoice = invoices.findById(INVOICE_1_ID).orElseThrow();
        invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
        invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
        invoice.setContentFilename("content.txt");
        invoice = invoices.save(invoice);

        INVOICE_1_VERSION = invoice.getVersion();
    }

    void checkETagExists(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(headers().etag().exists());
    }

    void checkETagUnchanged(String url, int original) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(headers().etag().isEqualTo(original));
    }

    void checkETagChanged(String url, int original) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(headers().etag().isNotEqualTo(original));
    }

    @AfterEach
    void cleanupTestData() {
        invoices.deleteAll();
        customers.deleteAll();
    }

    @Nested
    class ItemResource {

        @Test
        void getInvoice_withInvalidIfNoneMatch_http200() throws Exception {
            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_NONE_MATCH, INVALID_VERSION))
                    .andExpect(status().isOk())
                    .andExpect(headers().etag().isEqualTo(INVOICE_1_VERSION));
        }

        @Test
        void getInvoice_withMatchingIfNoneMatch_http304() throws Exception {
            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_NONE_MATCH, toETag(INVOICE_1_VERSION)))
                    .andExpect(status().isNotModified());
        }

        @Test
        void postInvoice_shouldSetETag() throws Exception {
            var response = mockMvc.perform(post("/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "I-2022-0003",
                                        "counterparty": "/customers/%s"
                                    }
                                    """.formatted(XENIT_ID)))
                    .andExpect(status().isCreated())
                    .andReturn();

            var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
            var invoiceId = StringUtils.substringAfterLast(location, "/");

            checkETagExists("/invoices/" + invoiceId);
        }

        @Test
        void putInvoice_withInvalidIfMatch_http412() throws Exception {
            mockMvc.perform(put("/invoices/{id}", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "%s",
                                        "paid": true
                                    }
                                    """.formatted(INVOICE_NUMBER_1)))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void putInvoice_withMatchingIfMatch_http200() throws Exception {
            mockMvc.perform(put("/invoices/{id}", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(INVOICE_1_VERSION))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "%s",
                                        "paid": true
                                    }
                                    """.formatted(INVOICE_NUMBER_1)))
                    .andExpect(status().isNoContent());

            checkETagChanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void deleteInvoice_withInvalidIfMatch_http412() throws Exception {
            mockMvc.perform(delete("/invoices/{id}", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void deleteInvoice_withMatchingIfMatch_http204() throws Exception {
            mockMvc.perform(delete("/invoices/{id}", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(INVOICE_1_VERSION)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/invoices/{id}", INVOICE_1_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class InlineContentProperty {

        @Test
        void postInvoiceContent_withMatchingIfMatch_http201() throws Exception {
            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(INVOICE_1_VERSION))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isCreated());

            checkETagChanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void postInvoiceContent_withInvalidIfMatch_http412() throws Exception {
            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION)
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        @Disabled("ACC-1313")
        void postMultipartInvoiceAndContent_shouldSetETag_http201() throws Exception {
            var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                    UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

            var response = mockMvc.perform(multipart(HttpMethod.POST, "/invoices")
                            .file(file)
                            .param("number", "I-2022-0003")
                            .param("counterparty", "/customers/" + XENIT_ID))
                    .andExpect(status().isCreated())
                    .andReturn();

            var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
            var invoiceId = StringUtils.substringAfterLast(location, "/");

            checkETagExists("/invoices/" + invoiceId);
        }

        @Test
        void putInvoiceContent_withMatchingIfMatch_http200() throws Exception {
            setupContentProperties();

            // update content, ONLY changing the charset
            mockMvc.perform(put("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(INVOICE_1_VERSION))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT))
                    .andExpect(status().isOk());

            checkETagChanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void putInvoiceContent_withInvalidIfMatch_http412() throws Exception {
            setupContentProperties();

            // update content, ONLY changing the charset
            mockMvc.perform(put("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void deleteInvoiceContent_withMatchingIfMatch_http200() throws Exception {
            setupContentProperties();

            mockMvc.perform(delete("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(INVOICE_1_VERSION)))
                    .andExpect(status().isNoContent());

            checkETagChanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }

        @Test
        void deleteInvoiceContent_withInvalidIfMatch_http412() throws Exception {
            setupContentProperties();

            mockMvc.perform(delete("/invoices/{id}/content", INVOICE_1_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/invoices/" + INVOICE_1_ID, INVOICE_1_VERSION);
        }
    }

    @Nested
    class EmbeddedContentProperty {

        @Test
        void postCustomerContent_withMatchingIfMatch_http201() throws Exception {
            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(XENIT_VERSION))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isCreated());

            checkETagChanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }

        @Test
        void postCustomerContent_withInvalidIfMatch_http412() throws Exception {
            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION)
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }

        @Test
        @Disabled("ACC-1313")
        void postMultipartCustomerAndContent_shouldSetETag_http201() throws Exception {
            var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                    UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

            var response = mockMvc.perform(multipart(HttpMethod.POST, "/customers")
                            .file(file)
                            .param("name", "Example")
                            .param("vat", "BE_EXAMPLE"))
                    .andExpect(status().isCreated())
                    .andReturn();

            var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
            var customerId = StringUtils.substringAfterLast(location, "/");

            checkETagExists("/customers/" + customerId);
        }

        @Test
        void putCustomerContent_withMatchingIfMatch_http200() throws Exception {
            setupContentProperties();

            // update content, ONLY changing the charset
            mockMvc.perform(put("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(XENIT_VERSION))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT))
                    .andExpect(status().isOk());

            checkETagChanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }

        @Test
        void putCustomerContent_withInvalidIfMatch_http412() throws Exception {
            setupContentProperties();

            // update content, ONLY changing the charset
            mockMvc.perform(put("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }

        @Test
        void deleteCustomerContent_withMatchingIfMatch_http200() throws Exception {
            setupContentProperties();

            mockMvc.perform(delete("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, toETag(XENIT_VERSION)))
                    .andExpect(status().isNoContent());

            checkETagChanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }

        @Test
        void deleteCustomerContent_withInvalidIfMatch_http412() throws Exception {
            setupContentProperties();

            mockMvc.perform(delete("/customers/{id}/content", XENIT_ID)
                            .header(HttpHeaders.IF_MATCH, INVALID_VERSION))
                    .andExpect(status().isPreconditionFailed());

            checkETagUnchanged("/customers/" + XENIT_ID, XENIT_VERSION);
        }
    }
}
