package com.contentgrid.spring.content.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.content.encryption.ContentEncryptionIntegrationTest.ContentEncryptionApplication;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.security.WithMockJwt;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.content.encryption.store.EncryptingContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.content.storage.type.default = fs",
        "contentgrid.thunx.abac.source = none"
})
@ContextConfiguration(classes = {
        InvoicingApplication.class,
        ContentEncryptionApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
public class ContentEncryptionIntegrationTest {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String INVOICE_NUMBER_2 = "I-2022-0002";
    static final String ORG_XENIT_VAT = "BE0887582365";
    static final String ORG_INBEV_VAT = "BE0417497106";
    static UUID INVOICE_1_ID;
    static UUID XENIT_ID;

    private static final String TEXT = "Some unicode text 💩";
    private static final byte[] CONTENT = TEXT.getBytes(StandardCharsets.UTF_8);
    private static final String MIMETYPE = "text/plain;charset=UTF-8";
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private EncryptedCustomerContentStore customerContentStore;

    @Autowired
    private EncryptedInvoiceContentStore invoiceContentStore;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private Stores stores;

    @BeforeEach
    void setup() {
        // Register EncryptedContentStoreResolver since there are multiple stores for Invoice and Customer
        var storeResolver = new EncryptedContentStoreResolver();
        stores.addStoreResolver("customers", storeResolver); // when resolving url path
        stores.addStoreResolver("invoices", storeResolver);
        stores.addStoreResolver(Customer.class.getCanonicalName(), storeResolver); // when creating content link for _links
        stores.addStoreResolver(Invoice.class.getCanonicalName(), storeResolver);

        // Create table "encryption"."dek_storage"
        dslContext.createSchemaIfNotExists("encryption").execute();
        dslContext.createTableIfNotExists(DSL.name("encryption", "dek_storage"))
                .column(DSL.field("content_id", String.class))
                .column(DSL.field("kek_label", String.class))
                .column(DSL.field("encrypted_dek", byte[].class))
                .column(DSL.field("algorithm", String.class))
                .column(DSL.field("iv", byte[].class))
                .primaryKey("content_id", "kek_label")
                .execute();

        // Create entities
        var customer = customerRepository.save(new Customer("xenit", ORG_XENIT_VAT));
        XENIT_ID = customer.getId();
        var invoice = invoiceRepository.save(new Invoice(INVOICE_NUMBER_1, false, false, customer, Set.of()));
        INVOICE_1_ID = invoice.getId();
    }

    @AfterEach
    void cleanUp() {
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Nested
    class InlineContent {

        @Test
        void createInvoice_withMultipart() throws Exception {
            var file = new MockMultipartFile("content", "example.txt", MIMETYPE, CONTENT);
            var response = mockMvc.perform(multipart(HttpMethod.POST, "/invoices")
                            .file(file)
                            .param("number", INVOICE_NUMBER_2)
                            .param("counterparty", "/customers/" + XENIT_ID))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse();

            var url = response.getHeader("Location");

            // Assert get returns unencrypted content
            mockMvc.perform(get(url + "/content"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(invoiceRepository.findByNumber(INVOICE_NUMBER_2)).hasValueSatisfying(invoice -> {
                assertThat(invoiceContentStore.getResource(invoice.getContentId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void postInvoiceContent() throws Exception {
            mockMvc.perform(post("/invoices/{id}/content", INVOICE_1_ID)
                            .contentType(MIMETYPE)
                            .content(CONTENT))
                    .andExpect(status().isCreated());

            // Assert get returns unencrypted content
            mockMvc.perform(get("/invoices/{id}/content", INVOICE_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(invoiceRepository.findByNumber(INVOICE_NUMBER_1)).hasValueSatisfying(invoice -> {
                assertThat(invoiceContentStore.getResource(invoice.getContentId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void putInvoiceContent() throws Exception {
            mockMvc.perform(put("/invoices/{id}/content", INVOICE_1_ID)
                            .contentType(MIMETYPE)
                            .content(CONTENT))
                    .andExpect(status().isCreated());

            // Assert get returns unencrypted content
            mockMvc.perform(get("/invoices/{id}/content", INVOICE_1_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(invoiceRepository.findByNumber(INVOICE_NUMBER_1)).hasValueSatisfying(invoice -> {
                assertThat(invoiceContentStore.getResource(invoice.getContentId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void deleteInvoiceContent() throws Exception {
            postInvoiceContent();

            mockMvc.perform(delete("/invoices/{id}/content", INVOICE_1_ID))
                    .andExpect(status().isNoContent());

            // Assert content is deleted
            mockMvc.perform(get("/invoices/{id}/content", INVOICE_1_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class EmbeddedContent {

        @Test
        void createCustomer_withMultipart() throws Exception {
            var file = new MockMultipartFile("content", "example.txt", MIMETYPE, CONTENT);
            var response = mockMvc.perform(multipart(HttpMethod.POST, "/customers")
                            .file(file)
                            .param("vat", ORG_INBEV_VAT))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse();

            var url = response.getHeader("Location");

            // Assert get returns unencrypted content
            mockMvc.perform(get(url + "/content"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(customerRepository.findByVat(ORG_INBEV_VAT)).hasValueSatisfying(customer -> {
                assertThat(customerContentStore.getResource(customer.getContent().getId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void postCustomerContent() throws Exception {
            mockMvc.perform(post("/customers/{id}/content", XENIT_ID)
                            .contentType(MIMETYPE)
                            .content(CONTENT))
                    .andExpect(status().isCreated());

            // Assert get returns unencrypted content
            mockMvc.perform(get("/customers/{id}/content", XENIT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(customerRepository.findByVat(ORG_XENIT_VAT)).hasValueSatisfying(customer -> {
                assertThat(customerContentStore.getResource(customer.getContent().getId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void putCustomerContent() throws Exception {
            mockMvc.perform(put("/customers/{id}/content", XENIT_ID)
                            .contentType(MIMETYPE)
                            .content(CONTENT))
                    .andExpect(status().isCreated());

            // Assert get returns unencrypted content
            mockMvc.perform(get("/customers/{id}/content", XENIT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(CONTENT));

            // Assert content is encrypted
            assertThat(customerRepository.findByVat(ORG_XENIT_VAT)).hasValueSatisfying(customer -> {
                assertThat(customerContentStore.getResource(customer.getContent().getId())).satisfies(resource -> {
                    assertThat(resource.getContentAsByteArray()).isNotEqualTo(CONTENT);
                });
            });
        }

        @Test
        void deleteCustomerContent() throws Exception {
            postCustomerContent();

            mockMvc.perform(delete("/customers/{id}/content", XENIT_ID))
                    .andExpect(status().isNoContent());

            // Assert content is deleted
            mockMvc.perform(get("/customers/{id}/content", XENIT_ID))
                    .andExpect(status().isNotFound());
        }
    }


    @SpringBootApplication
    public static class ContentEncryptionApplication {
        public static void main(String[] args) {
            SpringApplication.run(ContentEncryptionApplication.class, args);
        }

        @Configuration
        public static class Config {
            @Bean
            public EncryptingContentStoreConfigurer<EncryptedCustomerContentStore> customerContentStoreEncryptingContentStoreConfigurer(
                    DSLContext dslContext) {
                return new EncryptingContentStoreConfigurer<>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<EncryptedCustomerContentStore> config) {
                        config
                                .dataEncryptionKeyAccessor(
                                        new TableStorageDataEncryptionKeyAccessor<>(dslContext, "none"))
                                .unencryptedDataEncryptionKeys()
                        ;
                    }
                };
            }

            @Bean
            public EncryptingContentStoreConfigurer<EncryptedInvoiceContentStore> invocieContentStoreEncryptingContentStoreConfigurer(
                    DSLContext dslContext) {
                return new EncryptingContentStoreConfigurer<>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<EncryptedInvoiceContentStore> config) {
                        config
                                .dataEncryptionKeyAccessor(
                                        new TableStorageDataEncryptionKeyAccessor<>(dslContext, "none"))
                                .unencryptedDataEncryptionKeys()
                        ;
                    }
                };
            }
        }
    }

    public static class EncryptedContentStoreResolver implements StoreResolver {
        @Override
        public StoreInfo resolve(StoreInfo... stores) {
            if (stores.length == 0) {
                return null;
            } else if (stores.length == 1) {
                return stores[0];
            }

            for (var store : stores) {
                    for (var storeInterface : store.getInterface().getInterfaces()) {
                        if (storeInterface.equals(EncryptingContentStore.class)) {
                            return store;
                    }
                }
            }
            throw new IllegalArgumentException("No encrypted store found.");
        }
    }

    @StoreRestResource
    public interface EncryptedCustomerContentStore extends ContentStore<Customer, String>, EncryptingContentStore<Customer, String> {}

    @StoreRestResource
    public interface EncryptedInvoiceContentStore extends ContentStore<Invoice, String>, EncryptingContentStore<Invoice, String> {}
}
