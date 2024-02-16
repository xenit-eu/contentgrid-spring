package com.contentgrid.spring.test.fixture.invoicing;

import static com.contentgrid.spring.test.matchers.EtagHeaderMatcher.toEtag;
import static com.contentgrid.spring.test.matchers.ExtendedHeaderResultMatchers.headers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.OrderRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.PromotionCampaignRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.ShippingAddressRepository;
import com.contentgrid.spring.test.fixture.invoicing.store.CustomerContentStore;
import com.contentgrid.spring.test.fixture.invoicing.store.InvoiceContentStore;
import com.contentgrid.spring.test.security.WithMockJwt;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

@Slf4j
@SpringBootTest(properties = {
        "server.servlet.encoding.enabled=false" // disables mock-mvc enforcing charset in request
})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
@WithMockUser(username = "John")
class InvoicingApplicationTests {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String INVOICE_NUMBER_2 = "I-2022-0002";
    static final String INVOICE_NUMBER_3 = "I-2022-0003";

    static final String ORG_XENIT_VAT = "BE0887582365";
    static final String ORG_INBEV_VAT = "BE0417497106";
    static final String ORG_EXAMPLE_VAT = "BE0123456789";

    static UUID XENIT_ID, INBEV_ID;
    static UUID ORDER_1_ID, ORDER_2_ID;
    static UUID INVOICE_1_ID, INVOICE_2_ID;


    static String PROMO_XMAS, PROMO_SHIPPING, PROMO_CYBER;

    static UUID ADDRESS_ID_XENIT;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurieProvider curieProvider;

    @Autowired
    CustomerRepository customers;

    @Autowired
    InvoiceRepository invoices;

    @Autowired
    OrderRepository orders;

    @Autowired
    PromotionCampaignRepository promos;

    @Autowired
    ShippingAddressRepository shippingAddresses;

    @Autowired
    InvoiceContentStore invoicesContent;

    @Autowired
    CustomerContentStore customersContent;

    @Autowired
    PlatformTransactionManager transactionManager;


    void doInTransaction(ThrowingCallable callable) {
        new TransactionTemplate(this.transactionManager).execute(status -> {
            try {
                callable.call();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @BeforeEach
    void setupTestData() {
        PROMO_XMAS = promos.save(new PromotionCampaign("XMAS-2022", "10% off ")).getPromoCode();
        PROMO_SHIPPING = promos.save(new PromotionCampaign("FREE-SHIP", "Free Shipping")).getPromoCode();
        var promoCyber = promos.save(new PromotionCampaign("CYBER-MON", "Cyber Monday"));
        PROMO_CYBER = promoCyber.getPromoCode();

        var xenit = customers.save(new Customer(null, 0, null, null, null, null, "XeniT", ORG_XENIT_VAT, null, null, null, new HashSet<>(), new HashSet<>()));
        var inbev = customers.save(new Customer(null, 0, null, null, null, null, "AB InBev", ORG_INBEV_VAT, null, null, null, new HashSet<>(), new HashSet<>()));

        XENIT_ID = xenit.getId();
        INBEV_ID = inbev.getId();

        var address = shippingAddresses.save(new ShippingAddress("Diestsevest 32", "3000", "Leuven"));
        ADDRESS_ID_XENIT = address.getId();

        var order1 = orders.save(new Order(xenit, address, Set.of(promoCyber)));
        var order2 = orders.save(new Order(xenit));
        var order3 = orders.save(new Order(inbev));

        ORDER_1_ID = order1.getId();
        ORDER_2_ID = order2.getId();

        INVOICE_1_ID = invoices.save(
                new Invoice(INVOICE_NUMBER_1, true, false, xenit, new HashSet<>(List.of(order1, order2)))).getId();
        INVOICE_2_ID = invoices.save(new Invoice(INVOICE_NUMBER_2, false, true, inbev, new HashSet<>(List.of(order3))))
                .getId();

    }

    @AfterEach
    void cleanupTestData() {
        invoices.deleteAll();
        orders.deleteAll();
        shippingAddresses.deleteAll();
        customers.deleteAll();
        promos.deleteAll();
    }

    private Matcher<Object> curies() {
        var curies = ((List<Link>) curieProvider.getCurieInformation(Links.NONE))
                .stream()
                .map(curie -> Map.of(
                        "href", curie.getHref(),
                        "templated", curie.isTemplated(),
                        "name", curie.getName()
                ))
                .toList();
        return new BaseMatcher<Object>() {
            @Override
            public boolean matches(Object actual) {
                if (actual instanceof List<?> items) {
                    return curies.equals(items);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValueList("[", ", ", "]", curies);
            }
        };
    }

    @Nested
    class CollectionResource {

        @Nested
        @DisplayName("GET /{repository}/")
        class Get {

            @Test
            void listInvoices_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/invoices")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.size").value(20))
                        .andExpect(jsonPath("$.page.totalElements").value(2))
                        .andExpect(jsonPath("$.page.totalPages").value(1))
                        .andExpect(jsonPath("$.page.number").value(0))
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(2))
                        .andExpect(jsonPath("$._embedded.['item'][0].number").exists())
                        .andExpect(jsonPath("$._links.self.href").value("http://localhost/invoices?page=0&size=20"))
                        .andExpect(jsonPath("$._links.curies").value(curies()));
            }

            @Test
            void listInvoices_withFilter_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/invoices?number={number}", INVOICE_NUMBER_1)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(1))
                        .andExpect(jsonPath("$._embedded.['item'][0].number").value(INVOICE_NUMBER_1));
            }

            @Test
            void listInvoices_withFilter_ignoreCase_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/invoices?number={number}", INVOICE_NUMBER_1.toLowerCase(Locale.ROOT))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(1))
                        .andExpect(jsonPath("$._embedded.['item'][0].number").value(INVOICE_NUMBER_1));
            }

            @Test
            void listRefunds_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/refunds").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.size").value(20))
                        .andExpect(jsonPath("$.page.totalElements").value(0))
                        .andExpect(jsonPath("$.page.totalPages").value(0))
                        .andExpect(jsonPath("$.page.number").value(0))
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(0))
                        .andExpect(jsonPath("$._links.self.href").value("http://localhost/refunds?page=0&size=20"))
                        .andExpect(jsonPath("$._links.curies").value(curies()));
            }
        }

        @Nested
        @DisplayName("HEAD /{repository}/")
        class Head {

            @Test
            void checkInvoiceCollection_shouldReturn_http204_noContent() throws Exception {
                mockMvc.perform(head("/invoices")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNoContent());
            }
        }

        @Nested
        @DisplayName("POST /{repository}/")
        class Post {

            @Test
            void createInvoice_shouldReturn_http201_created() throws Exception {
                var customerId = customers.findByVat(ORG_XENIT_VAT).orElseThrow().getId();
                mockMvc.perform(post("/invoices")
                                .content("""
                                        {
                                            "number": "I-2022-0003",
                                            "counterparty": "/customers/%s"
                                        }
                                        """.formatted(customerId))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isCreated())
                        .andExpect(headers().location().path("/invoices/{id}"));
            }

            @Test
            @Disabled("Providing multi-value associations during creation is not possible")
            void createOrder_withPromoCodes_shouldReturn_http201_created() throws Exception {
                var customerId = customers.findByVat(ORG_XENIT_VAT).orElseThrow().getId();

                var result = mockMvc.perform(post("/orders")
                                .content("""
                                        {
                                            "customer": "/customers/%s",
                                            "_links": {
                                                "d:promos" : [
                                                    { "href": "/promotions/XMAS-2022" },
                                                    { "href": "/promotions/FREE-SHIP" }
                                                ]
                                            }
                                        }
                                        """.formatted(customerId))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isCreated())
                        .andExpect(headers().location().path("/orders/{id}"))
                        .andReturn();

                var orderId = Optional.ofNullable(result.getResponse().getHeader(HttpHeaders.LOCATION))
                        .map(location -> new UriTemplate("{scheme}://{host}/orders/{id}").match(location))
                        .map(matches -> matches.get("id"))
                        .map(UUID::fromString)
                        .orElseThrow();

                doInTransaction(() -> {
                    assertThat(orders.findById(orderId)).hasValueSatisfying(order -> {
                        assertThat(order.getPromos()).hasSize(2);
                    });
                });
            }
        }
    }

    @Nested
    class ItemResource {

        @Nested
        @DisplayName("GET /{repository}/{id}")
        class Get {

            @Test
            void getInvoice_shouldReturn_http200_ok() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.number").value(INVOICE_NUMBER_1))
                        .andExpect(jsonPath("$._links.curies").value(curies()))
                        .andExpect(headers().etag().isEqualTo(invoice.getVersion()));
            }

            @Test
            void getInvoice_withIfNoneMatch_http304() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                var prevVersion = invoice.getVersion();

                mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_NONE_MATCH, toEtag(prevVersion)))
                        .andExpect(status().isNotModified());
            }

            @Test
            void getInvoice_withOutdatedIfNoneMatch_http200_ok() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                var outdatedVersion = invoice.getVersion() - 1;

                mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_NONE_MATCH, toEtag(outdatedVersion)))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("HEAD /{repository}/{id}")
        class Head {

            @Test
            void headInvoice_shouldReturn_http204_noContent() throws Exception {
                mockMvc.perform(head("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNoContent());
            }


        }

        @Nested
        @DisplayName("PUT /{repository}/{id}")
        class Put {

            @Test
            void putInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "number": "%s",
                                            "paid": true
                                        }
                                        """.formatted(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isTrue();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

            @Test
            void putInvoice_withIfMatch_http204_ok() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, toEtag(invoice.getVersion()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "number": "%s",
                                            "paid": true
                                        }
                                        """.formatted(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());
                invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isTrue();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

            @Test
            void putInvoice_WithBadIfMatch_http412() throws Exception {
                mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "number": "%s",
                                            "paid": true
                                        }
                                        """.formatted(INVOICE_NUMBER_1)))
                        .andExpect(status().isPreconditionFailed());

                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isFalse();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

        }

        @Nested
        @DisplayName("PATCH /{repository}/{id}")
        class Patch {

            @Test
            void patchInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(patch("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "paid": true
                                        }
                                        """))
                        .andExpect(status().isNoContent());
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isTrue();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

            @Test
            void patchInvoice_withIfMatch_http204_ok() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                mockMvc.perform(patch("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, toEtag(invoice.getVersion()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "paid": true
                                        }
                                        """))
                        .andExpect(status().isNoContent());
                invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isTrue();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

            @Test
            void patchInvoice_withBadIfMatch_http412() throws Exception {
                mockMvc.perform(patch("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "paid": true
                                        }
                                        """))
                        .andExpect(status().isPreconditionFailed());
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                assertThat(invoice.isPaid()).isFalse();
                assertThat(invoice.getNumber()).isEqualTo(INVOICE_NUMBER_1);
            }

        }

        @Nested
        @DisplayName("DELETE /{repository}/{id}")
        class Delete {

            @Test
            void deleteInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());

                assertThat(invoices.findByNumber(INVOICE_NUMBER_1)).isEmpty();
            }

            @Test
            void deleteInvoice_withContent_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());

                assertThat(invoices.findByNumber(INVOICE_NUMBER_1)).isEmpty();
            }

            @Test
            void deleteInvoice_withIfMatch_http204_ok() throws Exception {
                var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, toEtag(invoice.getVersion())))
                        .andExpect(status().isNoContent());

                assertThat(invoices.findByNumber(INVOICE_NUMBER_1)).isEmpty();
            }

            @Test
            void deleteInvoice_withBadIfMatch_http412() throws Exception {
                mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                        .andExpect(status().isPreconditionFailed());

                assertThat(invoices.findByNumber(INVOICE_NUMBER_1)).isNotEmpty();
            }

        }
    }

    @Nested
    class AssociationResource {

        @Nested
        @DisplayName("GET /{repository}/{id}/{property}")
        class Get {

            @Nested
            class ManyToOne {

                @Test
                void getCustomer_forInvoice_shouldReturn_http302_redirect() throws Exception {

                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/customers/{id}", XENIT_ID));
                }
            }

            @Nested
            class OneToMany {

                @Test
                void getInvoices_forCustomer_shouldReturn_http302_redirect() throws Exception {

                    mockMvc.perform(get("/customers/" + customerIdByVat(ORG_XENIT_VAT) + "/invoices")
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(
                                    headers().location().uri("http://localhost/invoices?counterparty={id}", XENIT_ID));
                }

                @Test
                void getOrders_forInvoice_shouldReturn_http302_redirect() throws Exception {
                    mockMvc.perform(get("/invoices/{id}/orders", INVOICE_1_ID).accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/orders?invoice._id={id}", INVOICE_1_ID));
                }
            }

            @Nested
            class OneToOne {

                @Test
                void getShippingAddress_forOrder_shouldReturn_http302_redirect() throws Exception {
                    mockMvc.perform(get("/orders/{id}/shippingAddress", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location()
                                    .uri("http://localhost/shipping-addresses/{id}", ADDRESS_ID_XENIT));

                }
            }

            @Nested
            class ManyToMany {

                @Test
                void getPromos_forOrder_shouldReturn_http302_redirect() throws Exception {
                    mockMvc.perform(get("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/promotions?orders={id}", ORDER_1_ID));

                }
            }
        }

        @Nested
        @DisplayName("PUT /{repository}/{id}/{property}")
        class Put {

            @Nested
            class ManyToOne {

                @Test
                void putJson_shouldReturn_http204() throws Exception {
                    // fictive example: fix the customer
                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "customer" : {
                                                        "href": "/customers/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(correctCustomerId)))
                            .andExpect(status().isNoContent());
                }

                @Test
                void putJson_withIfMatch_http204() throws Exception {
                    // fictive example: fix the customer
                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    var prevVersion = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow().getVersion();
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .content("""
                                            {
                                                "_links": {
                                                    "customer" : {
                                                        "href": "/customers/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(correctCustomerId)))
                            .andExpect(status().isNoContent());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoice.getVersion()).isNotEqualTo(prevVersion);
                }

                @Test
                @Disabled("ACC-1188")
                void putJson_withIfBadMatch_http412() throws Exception {
                    // fictive example: fix the customer
                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    var prevVersion = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow().getVersion();
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .content("""
                                            {
                                                "_links": {
                                                    "customer" : {
                                                        "href": "/customers/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(correctCustomerId)))
                            .andExpect(status().isPreconditionFailed());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoice.getVersion()).isEqualTo(prevVersion);
                }

            }

            @Nested
            class OneToMany {

                @Test
                void putJson_shouldReplaceLinksAndReturn_http204_noContent() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });
                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);

                    // set the orders using PUT, using single-link object syntax
                    mockMvc.perform(put("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isNoContent());

                    // assert orders collection has been replaced
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders()).singleElement().satisfies(order -> {
                                assertThat(order.getId()).isEqualTo(newOrderId.get());
                            });
                        });
                    });
                }

                @Test
                void putJson_withIfMatch_http204_noContent() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });
                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);
                    var prevVersion = invoices.findById(invoiceNumber).orElseThrow().getVersion();

                    // set the orders using PUT, using single-link object syntax
                    mockMvc.perform(put("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isNoContent());

                    // assert orders collection has been replaced
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders()).singleElement().satisfies(order -> {
                                assertThat(order.getId()).isEqualTo(newOrderId.get());
                            });
                            assertThat(invoice.getVersion()).isNotEqualTo(prevVersion);
                        });
                    });
                }

                @Test
                @Disabled("ACC-1188")
                void putJson_withBadIfMatch_http412() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });
                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);
                    var prevVersion = invoices.findById(invoiceNumber).orElseThrow().getVersion();

                    // set the orders using PUT, using single-link object syntax
                    mockMvc.perform(put("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isPreconditionFailed());

                    // assert orders collection has been replaced
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders())
                                    .map(Order::getId)
                                    .containsExactlyInAnyOrder(ORDER_1_ID, ORDER_2_ID);
                            assertThat(invoice.getVersion()).isEqualTo(prevVersion);
                        });
                    });
                }

                @Test
                void putUriList_shouldReplaceLinksAndReturn_http204_noContent() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });
                    mockMvc.perform(put("/invoices/{id}/orders", INVOICE_1_ID)
                            .contentType(RestMediaTypes.TEXT_URI_LIST)
                            .content(
                                    """
                                    /orders/%s
                                    /orders/%s
                                    """.formatted(ORDER_1_ID, newOrderId)
                            )
                    ).andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        assertThat(invoices.findById(INVOICE_1_ID)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders())
                                    .map(Order::getId)
                                    .containsExactlyInAnyOrder(ORDER_1_ID, newOrderId.get());
                        });
                    });
                }
            }

            @Nested
            class OneToOne {

                @Test
                void putShippingAddress_forOrder_shouldReturn_http204_noContent() throws Exception {

                    var addressId = shippingAddresses.save(new ShippingAddress()).getId();

                    mockMvc.perform(put("/orders/{id}/shippingAddress", ORDER_2_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "shippingAddress" : {
                                                        "href": "/shipping-addresses/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(addressId)))
                            .andExpect(status().isNoContent());

                    var order = orders.findById(ORDER_2_ID).orElseThrow();
                    assertThat(order.getShippingAddress()).isNotNull();

                }

                @Test
                void putShippingAddress_forOrderWithIfMatch_http204_noContent() throws Exception {

                    var addressId = shippingAddresses.save(new ShippingAddress()).getId();
                    var prevVersion = orders.findById(ORDER_2_ID).orElseThrow().getVersion();

                    mockMvc.perform(put("/orders/{id}/shippingAddress", ORDER_2_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "shippingAddress" : {
                                                        "href": "/shipping-addresses/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(addressId)))
                            .andExpect(status().isNoContent());

                    var order = orders.findById(ORDER_2_ID).orElseThrow();
                    assertThat(order.getShippingAddress()).isNotNull();
                    assertThat(order.getVersion()).isNotEqualTo(prevVersion);
                }

                @Test
                @Disabled("ACC-1188")
                void putShippingAddress_forOrderWithBadIfMatch_http412() throws Exception {

                    var addressId = shippingAddresses.save(new ShippingAddress()).getId();
                    var prevVersion = orders.findById(ORDER_2_ID).orElseThrow().getVersion();

                    mockMvc.perform(put("/orders/{id}/shippingAddress", ORDER_2_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "shippingAddress" : {
                                                        "href": "/shipping-addresses/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(addressId)))
                            .andExpect(status().isPreconditionFailed());

                    var order = orders.findById(ORDER_2_ID).orElseThrow();
                    assertThat(order.getShippingAddress()).isNull();
                    assertThat(order.getVersion()).isEqualTo(prevVersion);
                }
            }

            @Nested
            class ManyToMany {

                @Test
                void putJson_emptyPromos_forOrder_shouldReturn_http204_noContent() throws Exception {
                    doInTransaction(() -> {
                        assertThat(orders.findById(ORDER_1_ID)).isNotNull()
                                .hasValueSatisfying(order -> assertThat(order.getPromos()).hasSize(1));
                    });

                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(""))
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        assertThat(orders.findById(ORDER_1_ID)).isNotNull()
                                .hasValueSatisfying(order -> assertThat(order.getPromos()).hasSize(0));
                    });
                }

                @Test
                void putJson_Promos_forOrder_shouldReturn_http204_noContent() throws Exception {
                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "promos" : [
                                                        { "href": "/promotions/XMAS-2022" },
                                                        { "href": "/promotions/FREE-SHIP" }
                                                    ]
                                                }
                                            }
                                            """)

                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(2);
                    });
                }

                @Test
                void putUriList_Promos_forOrder_shouldReturn_http204_noContent() throws Exception {
                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(RestMediaTypes.TEXT_URI_LIST_VALUE)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)
                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(2);
                    });
                }

                @Test
                void putUriList_Promos_forOrder_withIfMatch_http204_noContent() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .contentType(RestMediaTypes.TEXT_URI_LIST_VALUE)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)
                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(2);
                        assertThat(order.getVersion()).isNotEqualTo(prevVersion);
                    });
                }

                @Test
                @Disabled("ACC-1188")
                void putUriList_Promos_forOrder_withBadIfMatch_http412() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .contentType(RestMediaTypes.TEXT_URI_LIST_VALUE)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)
                            )
                            .andExpect(status().isPreconditionFailed());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(1);
                        assertThat(order.getVersion()).isEqualTo(prevVersion);
                    });
                }
            }
        }

        @Nested
        @DisplayName("POST /{repository}/{id}/{property}")
        class Post {

            @Nested
            class ManyToOne {

                @Test
                void postJson_shouldReturn_http405_methodNotAllowed() throws Exception {

                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "customer" : {
                                                        "href": "/customers/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(correctCustomerId)))
                            .andExpect(status().isMethodNotAllowed());
                }
            }

            @Nested
            class OneToMany {

                @Test
                void postJson_shouldAppend_http204_noContent() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });

                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);

                    // add an order to an invoice
                    mockMvc.perform(post("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isNoContent());

                    // assert orders collection has been augmented
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders())
                                    .hasSize(3)
                                    .anyMatch(order -> order.getId().equals(newOrderId.get()));
                        });
                    });
                }

                @Test
                void postJson_withIfMatch_shouldAppend_http204() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });

                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);
                    var prevVersion = invoices.findById(invoiceNumber).orElseThrow().getVersion();

                    // add an order to an invoice
                    mockMvc.perform(post("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isNoContent());

                    // assert orders collection has been augmented
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders())
                                    .hasSize(3)
                                    .anyMatch(order -> order.getId().equals(newOrderId.get()));
                            assertThat(invoice.getVersion()).isNotEqualTo(prevVersion);
                        });
                    });
                }

                @Test
                @Disabled("ACC-1188")
                void postJson_withBadIfMatch_http412() throws Exception {
                    AtomicReference<UUID> newOrderId = new AtomicReference<>(null);
                    doInTransaction(() -> {
                        var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                        newOrderId.set(orders.save(new Order(xenit)).getId());
                    });

                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);
                    var prevVersion = invoices.findById(invoiceNumber).orElseThrow().getVersion();

                    // add an order to an invoice
                    mockMvc.perform(post("/invoices/%s/orders".formatted(invoiceNumber))
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "orders" : {
                                                        "href": "/orders/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(newOrderId)))
                            .andExpect(status().isPreconditionFailed());

                    // assert orders collection has been augmented
                    doInTransaction(() -> {
                        assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                            assertThat(invoice.getOrders())
                                    .hasSize(2)
                                    .noneMatch(order -> order.getId().equals(newOrderId.get()));
                            assertThat(invoice.getVersion()).isEqualTo(prevVersion);
                        });
                    });
                }
            }

            @Nested
            class OneToOne {

                @Test
                void postShippingAddress_forOrder_shouldReturn_http405_methodNotAllowed() throws Exception {

                    var addressId = shippingAddresses.save(new ShippingAddress()).getId();

                    mockMvc.perform(post("/orders/{id}/shippingAddress", ORDER_2_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "shippingAddress" : {
                                                        "href": "/shipping-addresses/%s"
                                                    }
                                                }
                                            }
                                            """.formatted(addressId)))
                            .andExpect(status().isMethodNotAllowed());
                }
            }

            @Nested
            class ManyToMany {

                @Test
                void postJson_promos_forOrder_shouldAppendLinks_http204_noContent() throws Exception {
                    mockMvc.perform(post("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "_links": {
                                                    "promos" : [
                                                        { "href": "/promotions/XMAS-2022" },
                                                        { "href": "/promotions/FREE-SHIP" }
                                                    ]
                                                }
                                            }
                                            """)

                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(3);
                    });
                }

                @Test
                void postUriList_promos_forOrder_shouldAppendLinks_http204_noContent() throws Exception {
                    mockMvc.perform(post("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(RestMediaTypes.TEXT_URI_LIST)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)

                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(3);
                    });
                }

                @Test
                void postUriList_promos_forOrder_withIfMatch_http204_noContent() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(post("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .contentType(RestMediaTypes.TEXT_URI_LIST)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)

                            )
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(3);
                        assertThat(order.getVersion()).isNotEqualTo(prevVersion);
                    });
                }

                @Test
                @Disabled("ACC-1188")
                void postUriList_promos_forOrder_withBadIfMatch_http412() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(post("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .contentType(RestMediaTypes.TEXT_URI_LIST)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)

                            )
                            .andExpect(status().isPreconditionFailed());

                    doInTransaction(() -> {
                        var order = orders.findById(ORDER_1_ID).orElseThrow();
                        assertThat(order.getPromos()).hasSize(1);
                        assertThat(order.getVersion()).isEqualTo(prevVersion);
                    });
                }
            }
        }

        @Nested
        @DisplayName("DELETE /{repository}/{id}/{property}")
        class Delete {

            @Nested
            class ManyToOne {

                @Test
                void deleteOrderCustomer_shouldReturn_http204() throws Exception {

                    mockMvc.perform(delete("/orders/" + ORDER_1_ID + "/customer")
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> assertThat(order.getCustomer()).isNull());
                }

                @Test
                void deleteOrderCustomer_withIfMatch_http204() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(delete("/orders/" + ORDER_1_ID + "/customer")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> {
                                assertThat(order.getCustomer()).isNull();
                                assertThat(order.getVersion()).isNotEqualTo(prevVersion);
                            });
                }

                @Test
                @Disabled("ACC-1188")
                void deleteOrderCustomer_withBadIfMatch_http412() throws Exception {
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();
                    mockMvc.perform(delete("/orders/" + ORDER_1_ID + "/customer")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> {
                                assertThat(order.getCustomer()).isNotNull();
                                assertThat(order.getVersion()).isEqualTo(prevVersion);
                            });
                }
            }

            @Nested
            class OneToMany {

                @Test
                void deleteToManyAssoc_shouldReturn_http405_methodNotAllowed() throws Exception {

                    mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/orders")
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isMethodNotAllowed());
                }
            }

            @Nested
            class OneToOne {

                @Test
                void deleteShippingAddress_fromOrder_shouldReturn_http204() throws Exception {
                    assertThat(orders.findById(ORDER_1_ID)).hasValueSatisfying(order -> {
                        assertThat(order.getShippingAddress()).isNotNull();
                    });

                    mockMvc.perform(delete("/orders/{orderId}/shippingAddress", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> assertThat(order.getShippingAddress()).isNull());
                }

                @Test
                void deleteShippingAddress_fromOrder_withIfMatch_http204() throws Exception {
                    assertThat(orders.findById(ORDER_1_ID)).hasValueSatisfying(order -> {
                        assertThat(order.getShippingAddress()).isNotNull();
                    });
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();

                    mockMvc.perform(delete("/orders/{orderId}/shippingAddress", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> {
                                assertThat(order.getShippingAddress()).isNull();
                                assertThat(order.getVersion()).isNotEqualTo(prevVersion);
                            });
                }

                @Test
                @Disabled("ACC-1188")
                void deleteShippingAddress_fromOrder_withBadIfMatch_http412() throws Exception {
                    assertThat(orders.findById(ORDER_1_ID)).hasValueSatisfying(order -> {
                        assertThat(order.getShippingAddress()).isNotNull();
                    });
                    var prevVersion = orders.findById(ORDER_1_ID).orElseThrow().getVersion();

                    mockMvc.perform(delete("/orders/{orderId}/shippingAddress", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> {
                                assertThat(order.getShippingAddress()).isNotNull();
                                assertThat(order.getVersion()).isEqualTo(prevVersion);
                            });
                }
            }

            @Nested
            class ManyToMany {

                @Test
                void deletePromos_fromOrder_shouldReturn_http405_methodNotAllowed() throws Exception {
                    mockMvc.perform(delete("/orders/{orderId}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isMethodNotAllowed());
                }
            }
        }
    }


    @Nested
    class AssociationItemResource {

        @Nested
        @DisplayName("GET /{repository}/{entityId}/{property}/{propertyId}")
        class Get {

            @Nested
            class OneToMany {

                @Test
                void getInvoicesOrders_shouldReturn_http302() throws Exception {

                    mockMvc.perform(get("/invoices/{invoice}/orders/{order}", invoiceId(INVOICE_NUMBER_1), ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/orders/{id}", ORDER_1_ID));
                }
            }

            @Nested
            class ManyToOne {

                @Test
                void getInvoiceCustomerById_shouldReturn_http302() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_NUMBER_1).orElseThrow();
                    var counterPartyId = invoice.getCounterparty().getId();

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + counterPartyId)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/customers/%s".formatted(counterPartyId))));
                }

                @Test
                void getInvoiceCustomerByWrongId_shouldReturn_http404() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_NUMBER_1).orElseThrow();
                    var wrongCounterparty = customerIdByVat(ORG_INBEV_VAT);

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + wrongCounterparty)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNotFound());
                }
            }

            @Nested
            class ManyToMany {

                @Test
                @Disabled("See ACC-451")
                void getPromoById_forOrder_shouldReturn_http302_redirect() throws Exception {

                    promos.findByPromoCode(PROMO_CYBER).orElseThrow();

                    mockMvc.perform(get("/orders/{id}/promos/{promoCode}", ORDER_1_ID, PROMO_CYBER)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/promotions/{promoCode}", PROMO_XMAS));

                }

                @Test
                void getPromoById_forOrder_invalidId_shouldReturn_http404_notFound() throws Exception {
                    mockMvc.perform(get("/orders/{id}/promos/{promoCode}", ORDER_1_ID, PROMO_XMAS)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNotFound());

                }
            }
        }

        @Nested
        @DisplayName("DELETE /{repository}/{entityId}/{property}/{propertyId}")
        class Delete {

            @Nested
            class OneToMany {

                @Test
                void deleteOrderById_fromInvoice_shouldReturn_http204() throws Exception {
                    doInTransaction(() -> {
                        var invoice = invoices.findById(INVOICE_1_ID).orElseThrow();
                        assertThat(invoice.getOrders()).contains(orders.findById(ORDER_1_ID).orElseThrow());
                    });

                    mockMvc.perform(delete("/invoices/{invoice}/orders/{order}", INVOICE_1_ID, ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNoContent());

                    doInTransaction(() -> {
                        var invoice = invoices.findById(INVOICE_1_ID).orElseThrow();
                        assertThat(invoice.getOrders()).doesNotContain(orders.findById(ORDER_1_ID).orElseThrow());
                    });

                }
            }

            @Nested
            class OneToOne {

                @Test
                void deleteShippingAddressById_fromOrder_shouldReturn_http204() throws Exception {
                    assertThat(orders.findById(ORDER_1_ID)).hasValueSatisfying(order -> {
                        assertThat(order.getShippingAddress()).isNotNull();
                        assertThat(order.getShippingAddress().getId()).isEqualTo(ADDRESS_ID_XENIT);
                    });

                    mockMvc.perform(
                                    delete("/orders/{orderId}/shippingAddress/{addressId}", ORDER_1_ID, ADDRESS_ID_XENIT)
                                            .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> assertThat(order.getShippingAddress()).isNull());
                }

                @Test
                @Disabled("ACC-453")
                void deleteShippingAddressByWrongId_fromOrder_shouldReturn_http404() throws Exception {
                    assertThat(orders.findById(ORDER_1_ID)).hasValueSatisfying(order -> {
                        assertThat(order.getShippingAddress()).isNotNull();
                        assertThat(order.getShippingAddress().getId()).isEqualTo(ADDRESS_ID_XENIT);
                    });

                    mockMvc.perform(
                                    delete("/orders/{orderId}/shippingAddress/{addressId}", ORDER_1_ID, UUID.randomUUID())
                                            .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNotFound());

                }
            }
        }
    }


    @Nested
    class ContentPropertyResource {

        private static final String EXT_ASCII_TEXT = "L'éducation doit être gratuite.";
        private static final int EXT_ASCII_TEXT_LATIN1_LENGTH = 31;
        private static final int EXT_ASCII_TEXT_UTF8_LENGTH = 33;

        private static final String UNICODE_TEXT = "Some unicode text 💩";
        private static final int UNICODE_TEXT_UTF8_LENGTH = 18 + 4;

        private static final String MIMETYPE_PLAINTEXT_UTF8 = "text/plain;charset=UTF-8";
        private static final String MIMETYPE_PLAINTEXT_LATIN1 = "text/plain;charset=ISO-8859-1";

        @Nested
        class DirectProperty {

            @Nested
            @DisplayName("GET /{repository}/{entityId}/{contentProperty}")
            class Get {

                @Test
                void getInvoiceContent() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    invoice.setContentFilename(filename);
                    invoice = invoices.save(invoice);

                    var encodedFilename = UriUtils.encodeQuery(filename, StandardCharsets.UTF_8);
                    mockMvc.perform(get("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MIMETYPE_PLAINTEXT_UTF8))
                            .andExpect(content().string(EXT_ASCII_TEXT))
                            .andExpect(headers().etag().isEqualTo(invoice.getVersion()))
                    ;
                            /* This assertion is changed in SB3; and is technically incorrect
                            (it should be `Content-Disposition: attachment` or `Content-Disposition: inline` with a filename, never `form-data`)
                            .andExpect(headers().string("Content-Disposition",
                                    is("form-data; name=\"attachment\"; filename*=UTF-8''%s".formatted(encodedFilename)))) */
                    ;
                }

                @Test
                void getInvoiceContent_missingEntity_http404() throws Exception {
                    mockMvc.perform(get("/invoices/{id}/content", UUID.randomUUID())
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isNotFound());
                }

                @Test
                void getInvoiceContent_missingContent_http404() throws Exception {
                    mockMvc.perform(get("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isNotFound());
                }

                @Test
                void getInvoiceContent_withIfNoneMatch_http304() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    invoice.setContentFilename(filename);
                    invoice = invoices.save(invoice);

                    mockMvc.perform(get("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .accept(MediaType.ALL_VALUE)
                                    .header(HttpHeaders.IF_NONE_MATCH, toEtag(invoice.getVersion())))
                            .andExpect(status().isNotModified());
                }

                @Test
                void getInvoiceContent_withOutDatedIfNoneMatch_http200() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    invoice.setContentFilename(filename);
                    invoice = invoices.save(invoice);
                    var outdatedVersion = invoice.getVersion() - 1;

                    mockMvc.perform(get("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .accept(MediaType.ALL_VALUE)
                                    .header(HttpHeaders.IF_NONE_MATCH, toEtag(outdatedVersion)))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MIMETYPE_PLAINTEXT_UTF8))
                            .andExpect(content().string(EXT_ASCII_TEXT))
                            .andExpect(headers().etag().isNotEqualTo(outdatedVersion));
                }
            }

            @Nested
            @DisplayName("POST /{repository}/{entityId}/{contentProperty}")
            class Post {

                @Test
                void postInvoiceContent_textPlainUtf8_http201() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceContent_textPlainLatin1_http201() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceContent_textPlainLatin1_noCharset_http201() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType("text/plain")
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    // you have to "know" the charset encoding
                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));

                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceAttachment_secondaryContentProperty_http201() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/attachment", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("attachment")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getAttachmentId()).isNotBlank();
                    assertThat(invoice.getAttachmentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getAttachmentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getAttachmentFilename()).isNull();

                    assertThat(invoice.getContentId()).isNull();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceContent_update_http200() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setContentFilename("content.txt");
                    invoice = invoices.save(invoice);

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isOk());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);

                    // keeps original filename
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
                }

                @Test
                void postInvoiceContent_createWithIfMatch_http201() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isCreated());

                    // get invoice, expecting the etag to change
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceContent_createWithBadIfMatch_http412() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).isNull();
                    assertThat(invoice.getContentId()).isBlank();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void postInvoiceContent_updateWithIfMatch_http200() throws Exception {
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isOk());

                    // get invoice, expecting the etag to change
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);
                }

                @Test
                void postInvoiceContent_updateWithBadIfMatch_http412() throws Exception {
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void postInvoiceContent_missingEntity_http404() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/content", UUID.randomUUID())
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isNotFound());
                }

                @Test
                void postInvoiceContent_missingContentType_http400() throws Exception {
                    mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isBadRequest());
                }

                @Test
                void postMultipartContent_http201() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getContent(invoice)).isNull();

                    var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                    var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                    mockMvc.perform(multipart(HttpMethod.POST, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .file(file))
                            .andExpect(status().isCreated());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
                }

                @Test
                void postMultipartContent_updateDifferentContentType_http200() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var bytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), new ByteArrayInputStream(bytes));
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setContentFilename("content.txt");
                    invoices.save(invoice);

                    var image = new ClassPathResource("contentgrid-logo.png");
                    var content = image.getInputStream().readAllBytes();
                    var file = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, content);
                    mockMvc.perform(multipart(HttpMethod.POST, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .file(file))
                            .andExpect(status().isOk());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(content);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                    assertThat(invoice.getContentLength()).isEqualTo(image.contentLength());
                    assertThat(invoice.getContentFilename()).isEqualTo("logo.png");
                }

                @Test
                void postMultipartContent_noPayload_http400() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getContent(invoice)).isNull();

                    mockMvc.perform(multipart(HttpMethod.POST, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isBadRequest());
                }

                @Test
                void postMultipartEntityAndContent_textPlainUtf8_http201() throws Exception {
                    var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                            UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

                    mockMvc.perform(multipart(HttpMethod.POST, "/invoices")
                                    .file(file)
                                    .param("number", INVOICE_NUMBER_3)
                                    .param("counterparty", "/customers/" + customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_3)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo(file.getOriginalFilename());
                }

                @Test
                void postMultipartEntityAndContent_multipleContentProperties_http201() throws Exception {
                    var contentFile = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                            UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));
                    var attachmentFile = new MockMultipartFile("attachment", "attachment.txt",
                            MIMETYPE_PLAINTEXT_LATIN1, EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));

                    mockMvc.perform(multipart(HttpMethod.POST, "/invoices")
                                    .file(contentFile)
                                    .file(attachmentFile)
                                    .param("number", INVOICE_NUMBER_3)
                                    .param("counterparty", "/customers/" + customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_3)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo(contentFile.getOriginalFilename());
                    assertThat(invoice.getAttachmentId()).isNotBlank();
                    assertThat(invoice.getAttachmentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getAttachmentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getAttachmentFilename()).isEqualTo(attachmentFile.getOriginalFilename());
                }
            }

            @Nested
            @DisplayName("PUT /{repository}/{entityId}/{contentProperty}")
            class Put {

                @Test
                void putInvoiceContent_textPlainUtf8_http201() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceContent_textPlainLatin1_http201() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceContent_textPlainLatin1_noCharset_http201() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    // you have to "know" the charset encoding
                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));

                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceAttachment_secondaryContentProperty_http201() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/attachment", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("attachment")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getAttachmentId()).isNotBlank();
                    assertThat(invoice.getAttachmentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getAttachmentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getAttachmentFilename()).isNull();

                    assertThat(invoice.getContentId()).isNull();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceContent_update_http200() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setContentFilename("content.txt");
                    invoice = invoices.save(invoice);

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");

                    // update content, ONLY changing the charset
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isOk());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);

                    // keeps original filename
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
                }

                @Test
                void putInvoiceContent_createWithIfMatch_http201() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isCreated());

                    // get invoice, expecting the etag to change
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceContent_createWithBadIfMatch_http412() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).isNull();
                    assertThat(invoice.getContentId()).isBlank();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void putInvoiceContent_updateWithIfMatch_http200() throws Exception {
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    // update content, ONLY changing the charset
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isOk());

                    // get invoice, expecting the etag to change
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);
                }

                @Test
                void putInvoiceContent_updateWithBadIfMatch_http412() throws Exception {
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    // update content, ONLY changing the charset
                    mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void putInvoiceContent_missingEntity_http404() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/content", UUID.randomUUID())
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isNotFound());
                }

                @Test
                void putInvoiceContent_missingContentType_http400() throws Exception {
                    mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isBadRequest());
                }

                @Test
                void putMultipartContent_http201() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getContent(invoice)).isNull();

                    var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                    var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                    mockMvc.perform(multipart(HttpMethod.PUT, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .file(file))
                            .andExpect(status().isCreated());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
                }

                @Test
                void putMultipartContent_updateDifferentContentType_http200() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var bytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), new ByteArrayInputStream(bytes));
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setContentFilename("content.txt");
                    invoices.save(invoice);

                    var image = new ClassPathResource("contentgrid-logo.png");
                    var content = image.getInputStream().readAllBytes();
                    var file = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, content);
                    mockMvc.perform(multipart(HttpMethod.PUT, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .file(file))
                            .andExpect(status().isOk());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(content);
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                    assertThat(invoice.getContentLength()).isEqualTo(image.contentLength());
                    assertThat(invoice.getContentFilename()).isEqualTo("logo.png");
                }

                @Test
                void putMultipartContent_noPayload_http400() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getContent(invoice)).isNull();

                    mockMvc.perform(multipart(HttpMethod.PUT, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isBadRequest());
                }
            }

            @Nested
            @DisplayName("DELETE /{repository}/{entityId}/{contentProperty}")
            class Delete {

                @Test
                void deleteContent_http204() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var bytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                    invoicesContent.setContent(invoice, PropertyPath.from("content"), new ByteArrayInputStream(bytes));
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setContentFilename("content.txt");
                    invoices.save(invoice);

                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isNoContent());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var content = invoicesContent.getResource(invoice, PropertyPath.from("content"));
                    assertThat(content).isNull();

                    assertThat(invoice.getContentId()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void deleteContent_withIfMatch_http204() throws Exception {
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isNoContent());

                    // get invoice, expecting the etag to change
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoice.getContentId()).isNull();
                    assertThat(invoice.getContentLength()).isNull();
                    assertThat(invoice.getContentMimetype()).isNull();
                    assertThat(invoice.getContentFilename()).isNull();
                }

                @Test
                void deleteContent_withBadIfMatch_http412() throws Exception {
                    mockMvc.perform(post("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/content")
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();

                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();

                    assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(invoice.getContentId()).isNotBlank();
                    assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void deleteContent_noContent_http404() throws Exception {
                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isNotFound());
                }

                @Test
                void deleteContent_noEntity_http404() throws Exception {
                    mockMvc.perform(delete("/invoices/{id}/content", UUID.randomUUID()))
                            .andExpect(status().isNotFound());
                }

                @Test
                void deleteContent_noContentWithIfMatch_http404() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();
                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isNotFound());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                }

                @Test
                void deleteContent_noContentWithBadIfMatch_http404() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var prevVersion = invoice.getVersion();
                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isNotFound());

                    // get invoice, expecting the etag to be unchanged
                    mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                }

                @Test
                void deleteMultipleContentProperties() throws Exception {
                    var invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    var contentBytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                    invoicesContent.setContent(invoice, PropertyPath.from("content"),
                            new ByteArrayInputStream(contentBytes));
                    invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    invoice.setContentFilename("content.txt");

                    var attachmentBytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                    invoicesContent.setContent(invoice, PropertyPath.from("attachment"),
                            new ByteArrayInputStream(attachmentBytes));
                    invoice.setAttachmentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    invoice.setAttachmentFilename("attachment.txt");
                    invoice = invoices.save(invoice);

                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNotNull();
                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNotNull();

                    mockMvc.perform(delete("/invoices/{id}/attachment", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isNoContent());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNotNull();
                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNull();

                    mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                            .andExpect(status().isNoContent());

                    invoice = invoices.findById(invoiceId(INVOICE_NUMBER_1)).orElseThrow();
                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNull();
                    assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNull();
                }
            }
        }

        @Nested
        class EmbeddedProperty {

            @Nested
            @DisplayName("GET /{repository}/{entityId}/{contentProperty}")
            class Get {

                @Test
                void getCustomerContent() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    customersContent.setContent(customer, PropertyPath.from("content"), stream);
                    customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    customer.getContent().setFilename(filename);
                    customer = customers.save(customer);

                    var encodedFilename = UriUtils.encodeQuery(filename, StandardCharsets.UTF_8);
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MIMETYPE_PLAINTEXT_UTF8))
                            .andExpect(content().string(EXT_ASCII_TEXT))
                            .andExpect(headers().etag().isEqualTo(customer.getVersion()))
                    ;
                            /* This assertion is changed in SB3; and is technically incorrect
                            (it should be `Content-Disposition: attachment` or `Content-Disposition: inline` with a filename, never `form-data`)
                            .andExpect(headers().string("Content-Disposition",
                                    is("form-data; name=\"attachment\"; filename*=UTF-8''%s".formatted(encodedFilename)))) */
                    ;
                }

                @Test
                void getCustomerContent_missingEntity_http404() throws Exception {
                    mockMvc.perform(get("/customers/{id}/content", UUID.randomUUID())
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isNotFound());
                }

                @Test
                void getCustomerContent_missingContent_http404() throws Exception {
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .accept(MediaType.ALL_VALUE))
                            .andExpect(status().isNotFound());
                }

                @Test
                void getCustomerContent_withIfNoneMatch_http304() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    customersContent.setContent(customer, PropertyPath.from("content"), stream);
                    customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    customer.getContent().setFilename(filename);
                    customer = customers.save(customer);

                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .accept(MediaType.ALL_VALUE)
                                    .header(HttpHeaders.IF_NONE_MATCH, toEtag(customer.getVersion())))
                            .andExpect(status().isNotModified());
                }

                @Test
                void getCustomerContent_withOutdatedIfNoneMatch_http200() throws Exception {
                    var filename = "💩 and 📝.txt";
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                    customersContent.setContent(customer, PropertyPath.from("content"), stream);
                    customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_UTF8);
                    customer.getContent().setFilename(filename);
                    customer = customers.save(customer);
                    var outdatedVersion = customer.getVersion() - 1;

                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .accept(MediaType.ALL_VALUE)
                                    .header(HttpHeaders.IF_NONE_MATCH, toEtag(outdatedVersion)))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MIMETYPE_PLAINTEXT_UTF8))
                            .andExpect(content().string(EXT_ASCII_TEXT))
                            .andExpect(headers().etag().isNotEqualTo(outdatedVersion));
                }
            }

            @Nested
            @DisplayName("POST /{repository}/{entityId}/{contentProperty}")
            class Post {

                @Test
                void postCustomerContent_textPlainUtf8_http201() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isNull();
                }

                @Test
                void postCustomerContent_update_http200() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    customersContent.setContent(customer, PropertyPath.from("content"), stream);
                    customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    customer.getContent().setFilename("content.txt");
                    customer = customers.save(customer);

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                    assertThat(customer.getContent().getFilename()).isEqualTo("content.txt");

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isOk());

                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);

                    // keeps original filename
                    assertThat(customer.getContent().getFilename()).isEqualTo("content.txt");
                }

                @Test
                void postCustomerContent_createWithIfMatch_http201() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isCreated());

                    // get customer, expecting the etag to change
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isNull();
                }

                @Test
                void postCustomerContent_createWithBadIfMatch_http412() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get customer, expecting the etag to be unchanged
                    mockMvc.perform(get("/customers/{id}", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customer.getContent()).isNull();
                }

                @Test
                void postCustomerContent_updateWithIfMatch_http200() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isOk());

                    // get customer, expecting the etag to change
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);
                }

                @Test
                void postCustomerContent_updateWithBadIfMatch_http412() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);

                    // update content, ONLY changing the charset
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isPreconditionFailed());

                    // get customer, expecting the etag to be unchanged
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void postCustomerContent_missingEntity_http404() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", UUID.randomUUID())
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isNotFound());
                }

                @Test
                void postCustomerContent_missingContentType_http400() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isBadRequest());
                }

                @Test
                void postMultipartContent_http201() throws Exception {
                    var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                    var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                    mockMvc.perform(multipart("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .file(file))
                            .andExpect(status().isCreated());

                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isEqualTo(file.getOriginalFilename());
                }

                @Test
                void postMultipartEntityAndContent_textPlainUtf8_http201() throws Exception {
                    var file = new MockMultipartFile("content", "content.txt", MIMETYPE_PLAINTEXT_UTF8,
                            UNICODE_TEXT.getBytes(StandardCharsets.UTF_8));

                    mockMvc.perform(multipart(HttpMethod.POST, "/customers")
                                    .file(file)
                                    .param("name", "Example")
                                    .param("vat", ORG_EXAMPLE_VAT))
                            .andExpect(status().isCreated());

                    // Check whether customer exists
                    var customer = customers.findById(customerIdByVat(ORG_EXAMPLE_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isEqualTo(file.getOriginalFilename());
                }

            }

            @Nested
            @DisplayName("PUT /{repository}/{entityId}/{contentProperty}")
            class Put {

                @Test
                void putCustomerContent_textPlainUtf8_http201() throws Exception {
                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT))
                            .andExpect(status().isCreated());

                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isNull();
                }

                @Test
                void putCustomerContent_createWithIfMatch_http201() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isCreated());

                    // get customer, expecting the etag to change
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent()).isNotNull();
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isNull();
                }

                @Test
                void putCustomerContent_createWithBadIfMatch_http412() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(UNICODE_TEXT)
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get customer, expecting the etag to be unchanged
                    mockMvc.perform(get("/customers/{id}", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customer.getContent()).isNull();
                }

                @Test
                void putCustomerContent_updateWithIfMatch_http200() throws Exception {
                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);

                    // update content, ONLY changing the charset
                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion))
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isOk());

                    // get customer, expecting the etag to change
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(EXT_ASCII_TEXT);
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);
                }

                @Test
                void putCustomerContent_updateWithBadIfMatch_http412() throws Exception {
                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);

                    // update content, ONLY changing the charset
                    mockMvc.perform(put("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\"")
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT))
                            .andExpect(status().isPreconditionFailed());

                    // get customer, expecting the etag to be unchanged
                    mockMvc.perform(get("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void putMultipartContent_http201() throws Exception {
                    var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                    var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                    mockMvc.perform(multipart(HttpMethod.PUT, "/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .file(file))
                            .andExpect(status().isCreated());

                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasContent(UNICODE_TEXT);
                    assertThat(customer.getContent().getId()).isNotBlank();
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                    assertThat(customer.getContent().getLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                    assertThat(customer.getContent().getFilename()).isEqualTo(file.getOriginalFilename());
                }

            }

            @Nested
            @DisplayName("DELETE /{repository}/{entityId}/{contentProperty}")
            class Delete {

                @Test
                void deleteContent_http204() throws Exception {
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var bytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                    customersContent.setContent(customer, PropertyPath.from("content"), new ByteArrayInputStream(bytes));
                    customer.getContent().setMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                    customer.getContent().setFilename("content.txt");
                    customers.save(customer);

                    mockMvc.perform(delete("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(status().isNoContent());

                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var content = customersContent.getResource(customer, PropertyPath.from("content"));
                    assertThat(content).isNull();
                }

                @Test
                void deleteContent_withIfMatch_http204() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(delete("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, toEtag(prevVersion)))
                            .andExpect(status().isNoContent());

                    // get customer, expecting the etag to change
                    mockMvc.perform(get("/customers/{id}", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isNotEqualTo(prevVersion));

                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var content = customersContent.getResource(customer, PropertyPath.from("content"));
                    assertThat(content).isNull();
                }

                @Test
                void deleteContent_withBadIfMatch_http412() throws Exception {
                    mockMvc.perform(post("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .characterEncoding(StandardCharsets.ISO_8859_1)
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                            .andExpect(status().isCreated());
                    var customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();
                    var prevVersion = customer.getVersion();

                    mockMvc.perform(delete("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT))
                                    .header(HttpHeaders.IF_MATCH, "\"INVALID\""))
                            .andExpect(status().isPreconditionFailed());

                    // get customer, expecting the etag to be unchanged
                    mockMvc.perform(get("/customers/{id}", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(headers().etag().isEqualTo(prevVersion));
                    customer = customers.findById(customerIdByVat(ORG_XENIT_VAT)).orElseThrow();

                    assertThat(customersContent.getContent(customer, PropertyPath.from("content")))
                            .hasBinaryContent(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                    assertThat(customer.getContent().getMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_LATIN1);
                    assertThat(customer.getContent().getLength()).isEqualTo(EXT_ASCII_TEXT_LATIN1_LENGTH);
                }

                @Test
                void deleteContent_noContent_http404() throws Exception {
                    mockMvc.perform(delete("/customers/{id}/content", customerIdByVat(ORG_XENIT_VAT)))
                            .andExpect(status().isNotFound());
                }

                @Test
                void deleteContent_noEntity_http404() throws Exception {
                    mockMvc.perform(delete("/customers/{id}/content", UUID.randomUUID()))
                            .andExpect(status().isNotFound());
                }

            }
        }
    }

    private UUID invoiceId(String number) {
        return invoices.findByNumber(number).map(Invoice::getId).orElseThrow();
    }

    private UUID customerIdByVat(String vat) {
        return customers.findByVat(vat).map(Customer::getId).orElseThrow();
    }


}
