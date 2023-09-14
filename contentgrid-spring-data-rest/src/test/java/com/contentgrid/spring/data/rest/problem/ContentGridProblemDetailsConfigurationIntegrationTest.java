package com.contentgrid.spring.data.rest.problem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Refund;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.RefundRepository;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * All cases to be covered - Invalid json (no domain object can be constructed) - Validator violations (domain object
 * constructed; required attribute/relation missing) - Deletion violations (on delete object is still referenced by
 * required relation) - Database constraint errors (non-validation covered constraints)
 */
@SpringBootTest(classes = InvoicingApplication.class)
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class ContentGridProblemDetailsConfigurationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    public static final String CUSTOMER_ID_CREATE = "bd5ef028-52fb-11ee-a531-b3ff1a44e992";
    public static final String INVOICE_ID_CREATE = "bd5ef028-52fb-11ee-a531-b3ff1a44e993";

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    private Customer createCustomer() {
        var customer = new Customer();
        customer.setVat("vat-" + UUID.randomUUID());
        return customerRepository.save(customer);
    }

    private Invoice createInvoice() {
        var invoice = new Invoice();
        invoice.setNumber("invoice-" + UUID.randomUUID());
        invoice.setCounterparty(createCustomer());
        return invoiceRepository.save(invoice);
    }

    @AfterEach
    void cleanup(@Autowired CustomerRepository customerRepository, @Autowired InvoiceRepository invoiceRepository) {
        invoiceRepository.deleteById(UUID.fromString(INVOICE_ID_CREATE));
        customerRepository.deleteById(UUID.fromString(CUSTOMER_ID_CREATE));
    }

    /**
     * Tests all invalid json input cases:
     *
     * <ul>
     * <li>Does not parse as json
     * <li>Type mismatch: trying to use a string for a number
     * <li>Type mismatch: trying to use a string for an object
     * <li>Type mismatch: string does not parse to a date
     * <li>to-one relation input: not a valid URL
     * <li>to-one relation input: URL to a different entity
     * </ul>
     */
    @Nested
    class InvalidJson {

        public static String CUSTOMER_ID_UPDATE;
        public static String INVOICE_ID_UPDATE;

        @BeforeAll
        static void setup(@Autowired CustomerRepository customerRepository,
                @Autowired InvoiceRepository invoiceRepository) {
            var customer = new Customer();
            customer.setVat("vat-" + UUID.randomUUID());
            customer = customerRepository.save(customer);
            CUSTOMER_ID_UPDATE = customer.getId().toString();

            var invoice = new Invoice();
            invoice.setNumber("invoice-" + UUID.randomUUID());
            invoice.setCounterparty(customer);
            invoice = invoiceRepository.save(invoice);
            INVOICE_ID_UPDATE = invoice.getId().toString();
        }

        @ParameterizedTest
        @MethodSource({"basicUrls"})
        void doesNotParseAsJson(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "my-invalid-json }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource({"basicUrls"})
        void typeMismatchStringToNumber(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "123",
                                        "total_spend": "none yet"
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());

        }

        @ParameterizedTest
        @MethodSource({"basicUrls"})
        void typeMismatchStringToObject(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "456",
                                        "content": "XYZ"
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource({"basicUrls"})
        void typeMismatchStringDoesNotParseToDate(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "789",
                                        "birthday": "2022-01-01"
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource("relationUrls")
        void toOneRelationInvalidUrl(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "1123",
                                        "counterparty": "ZZEY"
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource("relationUrls")
        void toOneRelationDifferentEntityUrl(HttpMethod method, String url) throws Exception {
            mockMvc.perform(request(method, url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "1124",
                                        "counterparty": "/invoices/%s"
                                    }
                                    """.formatted(INVOICE_ID_UPDATE))
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        static Stream<Arguments> basicUrls() {
            return Stream.of(
                    Arguments.of(HttpMethod.POST, "/customers"),
                    Arguments.of(HttpMethod.PUT, "/customers/" + CUSTOMER_ID_CREATE),
                    Arguments.of(HttpMethod.PUT, "/customers/" + CUSTOMER_ID_UPDATE),
                    Arguments.of(HttpMethod.PATCH, "/customers/" + CUSTOMER_ID_UPDATE)
            );
        }

        static Stream<Arguments> relationUrls() {
            return Stream.of(
                    Arguments.of(HttpMethod.POST, "/invoices"),
                    Arguments.of(HttpMethod.PUT, "/invoices/" + INVOICE_ID_CREATE),
                    Arguments.of(HttpMethod.PUT, "/invoices/" + INVOICE_ID_UPDATE),
                    Arguments.of(HttpMethod.PATCH, "/invoices/" + INVOICE_ID_UPDATE)
            );
        }

    }

    /**
     * Tests all validator violation cases:
     * <ul>
     * <li>Create entity without required attribute
     * <li>Create entity without required (-to-one) relation
     * <li>Update entity to remove/null required attribute
     * <li>Update entity to remove/null required relation
     * <li>Remove entity relation that is required on this side
     * <li>Remove entity relation that is the target of a required one-to-one relation
     * </ul>
     */
    @Nested
    class ValidatorViolations {

        @Test
        void createEntityWithoutRequiredAttribute() throws Exception {
            var customer = createCustomer();
            mockMvc.perform(post("/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "counterparty": "/customers/%s"
                                    }
                                    """.formatted(customer.getId()))
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        void createEntityWithoutRequiredRelation() throws Exception {
            mockMvc.perform(post("/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": "%s"
                                    }
                                    """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        void updateEntityRemoveRequiredAttribute() throws Exception {
            var invoice = createInvoice();
            var customer = createCustomer();
            mockMvc.perform(put("/invoices/{id}", invoice.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "counterparty": "/customers/%s"
                                    }
                                    """.formatted(customer.getId()))
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());

            mockMvc.perform(patch("/invoices/{id}", invoice.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "number": null
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        void updateEntityRemoveRequiredRelation() throws Exception {
            // for PUT, relations are ignored if they are not present
            /*
            mockMvc.perform(put("/invoices/{id}", INVOICE_ID_UPDATE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {
                                "number": "%s"
                            }
                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
             */

            var invoice = createInvoice();
            mockMvc.perform(patch("/invoices/{id}", invoice.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "counterparty": null
                                    }
                                    """)
                    )
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        void removeRequiredEntityRelation_thisSide() throws Exception {
            var invoice = createInvoice();
            mockMvc.perform(delete("/invoices/{id}/counterparty", invoice.getId()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @Disabled("Currently inverse-side updates are not persisted anyways")
        void removeRequiredEntityRelation_otherSide(@Autowired RefundRepository refundRepository) throws Exception {
            var invoice = createInvoice();
            var refund = new Refund();
            refund.setInvoice(createInvoice());
            refundRepository.save(refund);
            // Now there is a refund that references our invoice

            mockMvc.perform(delete("/invoices/{id}/refund", invoice.getId()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    /**
     * Tests all deletion violation cases:
     * <ul>
     * <li>Delete entity that is the target of a required many-to-one relation
     * <li>Delete entity that is the target of a required one-to-one relation
     * </ul>
     */
    @Nested
    class DeletionViolations {

        @Test
        void deleteEntity_targetOfRequiredManyToOneRelation() throws Exception {
            var invoice = createInvoice();
            // This customer is linked to the invoice
            mockMvc.perform(delete("/customers/{id}", invoice.getCounterparty().getId()))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        void deleteEntity_targetOfRequiredOneToOneRelation(@Autowired CustomerRepository customerRepository,
                @Autowired InvoiceRepository invoiceRepository, @Autowired RefundRepository refundRepository)
                throws Exception {
            var invoice = createInvoice();

            var refund = new Refund();
            refund.setInvoice(invoice);
            refundRepository.save(refund);
            // Now there is a refund that references our invoice

            mockMvc.perform(delete("/invoices/{id}", invoice.getId()))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

    }

    /**
     * Tests all database constraint cases:
     * <ul>
     * <li>Unique constraint violations (unique column value created/updated for the second time)
     * <li>FK constraint violations
     * </ul>
     */
    @Nested
    class DatabaseConstraintViolations {

        @Test
        void uniqueConstraintViolation_create() throws Exception {
            var customerVat = UUID.randomUUID();

            // First time goes through
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "%s"
                                    }
                                    """.formatted(customerVat))
                    )
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            // Second time results in a unique constraint error
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "%s"
                                    }
                                    """.formatted(customerVat))
                    )
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        void uniqueConstraintViolation_update() throws Exception {
            var customer = createCustomer();
            var customerVat = UUID.randomUUID();

            // Create goes through
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "%s"
                                    }
                                    """.formatted(customerVat))
                    )
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            // Update to same id fails
            mockMvc.perform(patch("/customers/{id}", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "vat": "%s"
                                    }
                                    """.formatted(customerVat))
                    )
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        @Disabled("cases are covered by constraints, so no direct way to check it")
        void foreignKeyConstraintViolation() {

        }

    }

}