package com.contentgrid.spring.data.rest.auditing;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.data.support.auditing.v1.AuditMetadata;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt(subject = "user-id-1", name = "John", issuer = AuditMetadataTest.JWT_ISSUER_NAMESPACE)
public class AuditMetadataTest {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String ORG_XENIT_VAT = "BE0887582365";
    static final String JWT_ISSUER_NAMESPACE = "http://localhost/realms/cg-invalid";

    static UUID XENIT_ID;
    static UUID INVOICE_1_ID;

    static Instant TIMESTAMP;
    static Instant CONTENT_TIMESTAMP;

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

    @MockBean(name = "mockedDateTimeProvider")
    DateTimeProvider mockedDateTimeProvider;

    @SpyBean
    private AuditingHandler auditingHandler;

    String formatInstant(Instant date) {
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("GMT"));
        return formatter.format(date);
    }

    static JwtRequestPostProcessor jwtWithClaims(String subject, String name) {
        return jwt().jwt(jwt -> jwt
                .subject(subject)
                .claim("name", name)
                .issuer(JWT_ISSUER_NAMESPACE)
        );
    }

    @BeforeEach
    void setupTestData() {
        auditingHandler.setDateTimeProvider(mockedDateTimeProvider);
        TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(TIMESTAMP));

        var xenit = customers.save(
                new Customer(null, 0, new AuditMetadata(), "XeniT", ORG_XENIT_VAT, null, null, null, new HashSet<>(),
                        new HashSet<>()));

        XENIT_ID = xenit.getId();

        INVOICE_1_ID = invoices.save(
                new Invoice(INVOICE_NUMBER_1, true, false, xenit, new HashSet<>())).getId();
    }

    void setupContentProperties() {
        CONTENT_TIMESTAMP = TIMESTAMP.plus(1, ChronoUnit.SECONDS);
        Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(CONTENT_TIMESTAMP));
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
    }

    @AfterEach
    void cleanupTestData() {
        invoices.deleteAll();
        customers.deleteAll();
    }

    void checkInvoiceAuditMetadataFields(UUID id, String createdBy, Instant createdDate) throws Exception {
        checkInvoiceAuditMetadataFields(id, createdBy, createdDate, createdBy, createdDate);
    }

    void checkInvoiceAuditMetadataFields(
            UUID id, String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate
    ) throws Exception {
        checkAuditMetadataFields("invoices", id, createdBy, createdDate, lastModifiedBy, lastModifiedDate);
    }

    void checkCustomerAuditMetadataFields(UUID id, String createdBy, Instant createdDate) throws Exception {
        checkCustomerAuditMetadataFields(id, createdBy, createdDate, createdBy, createdDate);
    }

    void checkCustomerAuditMetadataFields(
            UUID id, String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate
    ) throws Exception {
        checkAuditMetadataFields("customers", id, createdBy, createdDate, lastModifiedBy, lastModifiedDate);
    }

    void checkAuditMetadataFields(String repository, UUID id, String createdBy, Instant createdDate,
            String lastModifiedBy, Instant lastModifiedDate) throws Exception {
        mockMvc.perform(get("/{repository}/{id}", repository, id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            audit_metadata: {
                                created_by: "%s",
                                created_date: "%s",
                                last_modified_by: "%s",
                                last_modified_date: "%s"
                            }
                        }
                        """.formatted(createdBy, createdDate, lastModifiedBy, lastModifiedDate)))
                .andExpect(header().dateValue(HttpHeaders.LAST_MODIFIED, lastModifiedDate.toEpochMilli()));
    }

    @Nested
    class ItemResource {

        @Test
        void getInvoice_withOutdatedIfModifiedSince_http200() throws Exception {
            var headerDate = TIMESTAMP.minus(1, ChronoUnit.HOURS);

            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());
        }

        @Test
        void getInvoice_withRecentIfModifiedSince_http304() throws Exception {
            var headerDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);

            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_MODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNotModified());
        }

        @Test
        void getInvoice_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = TIMESTAMP.minus(1, ChronoUnit.HOURS);

            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());
        }

        @Test
        void getInvoice_withRecentIfUnmodifiedSince_http200() throws Exception {
            var headerDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);

            mockMvc.perform(get("/invoices/" + INVOICE_1_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());
        }

        @Test
        void postInvoice_shouldSetAuditMetadataFields() throws Exception {
            var createdDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(createdDate));

            var response = mockMvc.perform(post("/invoices")
                            .with(jwtWithClaims("user-id-2", "Bob"))
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

            checkInvoiceAuditMetadataFields(UUID.fromString(invoiceId), "Bob", createdDate);
        }

        @Test
        @Disabled("ACC-1220")
        void putInvoice_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(put("/invoices/{id}", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "%s",
                                        "paid": true
                                    }
                                    """.formatted(INVOICE_NUMBER_1)))
                    .andExpect(status().isPreconditionFailed());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP);
        }

        @Test
        void putInvoice_withRecentIfUnmodifiedSince_http200() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(put("/invoices/{id}", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "%s",
                                        "paid": true
                                    }
                                    """.formatted(INVOICE_NUMBER_1)))
                    .andExpect(status().isNoContent());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        @Disabled("ACC-1220")
        void deleteInvoice_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var headerDate = TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/invoices/{id}", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP);
        }

        @Test
        void deleteInvoice_withRecentIfUnmodifiedSince_http204() throws Exception {
            var headerDate = TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/invoices/{id}", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class InlineContentProperty {

        @Test
        @Disabled("ACC-1312")
        void postInvoiceContent_withRecentIfUnmodifiedSince_http201() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isCreated());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void postInvoiceContent_withoutIfUnmodifiedSince_http201() throws Exception {
            // Previous test without header to test that auditMetadata fields are updated after creating content
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isCreated());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void postInvoiceContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP);
        }

        @Test
        @Disabled("ACC-1313")
        void postMultipartInvoiceAndContent_shouldSetAuditMetadataFields_http201() throws Exception {
            var createdDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(createdDate));
            var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                    UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

            var response = mockMvc.perform(multipart(HttpMethod.POST, "/invoices")
                            .file(file)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .param("number", "I-2022-0003")
                            .param("counterparty", "/customers/" + XENIT_ID))
                    .andExpect(status().isCreated())
                    .andReturn();

            var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
            var invoiceId = StringUtils.substringAfterLast(location, "/");

            checkInvoiceAuditMetadataFields(UUID.fromString(invoiceId), "Bob", createdDate);
        }

        @Test
        void putInvoiceContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void putInvoiceContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "John", CONTENT_TIMESTAMP);
        }

        @Test
        void deleteInvoiceContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNoContent());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void deleteInvoiceContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/invoices/{id}/content", INVOICE_1_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkInvoiceAuditMetadataFields(INVOICE_1_ID, "John", TIMESTAMP, "John", CONTENT_TIMESTAMP);
        }
    }

    @Nested
    class EmbeddedContentProperty {

        @Test
        @Disabled("ACC-1312")
        void postCustomerContent_withRecentIfUnmodifiedSince_http201() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isCreated());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void postCustomerContent_withoutIfUnmodifiedSince_http201() throws Exception {
            // Previous test without header to test that auditMetadata fields are updated after creating content
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT))
                    .andExpect(status().isCreated());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void postCustomerContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            var modifiedDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .contentType(MediaType.TEXT_PLAIN)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(UNICODE_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP);
        }

        @Test
        @Disabled("ACC-1313")
        void postMultipartCustomerAndContent_shouldSetAuditMetadataFields_http201() throws Exception {
            var createdDate = TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(createdDate));
            var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                    UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

            var response = mockMvc.perform(multipart(HttpMethod.POST, "/customers")
                            .file(file)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .param("name", "Example")
                            .param("vat", "BE_EXAMPLE"))
                    .andExpect(status().isCreated())
                    .andReturn();

            var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
            var customerId = StringUtils.substringAfterLast(location, "/");

            checkCustomerAuditMetadataFields(UUID.fromString(customerId), "Bob", createdDate);
        }

        @Test
        void putCustomerContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isOk());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void putCustomerContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            // update content, ONLY changing the charset
            mockMvc.perform(put("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(EXT_ASCII_TEXT)
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "John", CONTENT_TIMESTAMP);
        }

        @Test
        void deleteCustomerContent_withRecentIfUnmodifiedSince_http200() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isNoContent());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "Bob", modifiedDate);
        }

        @Test
        void deleteCustomerContent_withOutdatedIfUnmodifiedSince_http412() throws Exception {
            setupContentProperties();
            var modifiedDate = CONTENT_TIMESTAMP.plus(1, ChronoUnit.HOURS);
            Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(modifiedDate));

            var headerDate = CONTENT_TIMESTAMP.minus(1, ChronoUnit.MINUTES);

            mockMvc.perform(delete("/customers/{id}/content", XENIT_ID)
                            .with(jwtWithClaims("user-id-2", "Bob"))
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, formatInstant(headerDate)))
                    .andExpect(status().isPreconditionFailed());

            checkCustomerAuditMetadataFields(XENIT_ID, "John", TIMESTAMP, "John", CONTENT_TIMESTAMP);
        }
    }
}
