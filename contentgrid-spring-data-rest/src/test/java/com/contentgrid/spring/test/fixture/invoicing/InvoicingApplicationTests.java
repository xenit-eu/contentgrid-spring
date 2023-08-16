package com.contentgrid.spring.test.fixture.invoicing;

import static com.contentgrid.spring.test.matchers.ExtendedHeaderResultMatchers.headers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
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

import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import com.contentgrid.spring.test.fixture.invoicing.model.QOrder;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.OrderRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.PromotionCampaignRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.ShippingAddressRepository;
import com.contentgrid.spring.test.fixture.invoicing.store.InvoiceContentStore;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

@Slf4j
@Transactional
@SpringBootTest(properties = {
        "server.servlet.encoding.enabled=false" // disables mock-mvc enforcing charset in request
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class InvoicingApplicationTests {

    static final String INVOICE_NUMBER_1 = "I-2022-0001";
    static final String INVOICE_NUMBER_2 = "I-2022-0002";

    static final String ORG_XENIT_VAT = "BE0887582365";
    static final String ORG_INBEV_VAT = "BE0417497106";

    static UUID XENIT_ID, INBEV_ID;
    static UUID ORDER_1_ID, ORDER_2_ID;
    static UUID INVOICE_1_ID, INVOICE_2_ID;


    static String PROMO_XMAS, PROMO_SHIPPING, PROMO_CYBER;

    static UUID ADDRESS_ID_XENIT;

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void setupTestData() {
        PROMO_XMAS = promos.save(new PromotionCampaign("XMAS-2022", "10% off ")).getPromoCode();
        PROMO_SHIPPING = promos.save(new PromotionCampaign("FREE-SHIP", "Free Shipping")).getPromoCode();
        var promoCyber = promos.save(new PromotionCampaign("CYBER-MON", "Cyber Monday"));
        PROMO_CYBER = promoCyber.getPromoCode();

        var xenit = customers.save(new Customer(null, "XeniT", ORG_XENIT_VAT, null, new HashSet<>(), new HashSet<>()));
        var inbev = customers.save(new Customer(null, "AB InBev", ORG_INBEV_VAT, null, new HashSet<>(), new HashSet<>()));

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

    @Nested
    class CollectionResource {

        @Nested
        @DisplayName("GET /{repository}/")
        class Get {

            @Test
            void listInvoices_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/invoices")
                                .contentType("application/json"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(2))
                        .andExpect(jsonPath("$._embedded.['item'][0].number").exists())
                        .andExpect(jsonPath("$._links.self.href").value("http://localhost/invoices"));
            }

            @Test
            void listRefunds_returns_http200_ok() throws Exception {
                mockMvc.perform(get("/refunds").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.['item'].length()").value(0))
                        .andExpect(jsonPath("$._links.self.href").value("http://localhost/refunds"));
            }
        }

        @Nested
        @DisplayName("HEAD /{repository}/")
        class Head {

            @Test
            void checkInvoiceCollection_shouldReturn_http204_noContent() throws Exception {
                mockMvc.perform(head("/invoices")
                                .contentType("application/json"))
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
                                .contentType("application/json"))
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
                                .contentType("application/json"))
                        .andExpect(status().isCreated())
                        .andExpect(headers().location().path("/orders/{id}"))
                        .andReturn();

                var orderId = Optional.ofNullable(result.getResponse().getHeader(HttpHeaders.LOCATION))
                        .map(location -> new UriTemplate("{scheme}://{host}/orders/{id}").match(location))
                        .map(matches -> matches.get("id"))
                        .map(UUID::fromString)
                        .orElseThrow();

                assertThat(orders.findById(orderId)).hasValueSatisfying(order -> {
                    assertThat(order.getPromos()).hasSize(2);
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

                mockMvc.perform(get("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType("application/json"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.number").value(INVOICE_NUMBER_1));
            }

        }

        @Nested
        @DisplayName("HEAD /{repository}/{id}")
        class Head {

            @Test
            void headInvoice_shouldReturn_http204_noContent() throws Exception {
                mockMvc.perform(head("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType("application/json"))
                        .andExpect(status().isNoContent());
            }


        }

        @Nested
        @DisplayName("PUT /{repository}/{id}")
        class Put {

            @Test
            void putInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(put("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType("application/json")
                                .content("""
                                        {
                                            "number": "%s",
                                            "paid": true
                                        }
                                        """.formatted(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());
            }

        }

        @Nested
        @DisplayName("PATCH /{repository}/{id}")
        class Patch {

            @Test
            void patchInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(patch("/invoices/" + invoiceId(INVOICE_NUMBER_1))
                                .contentType("application/json")
                                .content("""
                                        {
                                            "paid": true
                                        }
                                        """))
                        .andExpect(status().isNoContent());
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
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/customers/{id}", XENIT_ID));
                }
            }

            @Nested
            class OneToMany {

                @Test
                void getInvoices_forCustomer_shouldReturn_http302_redirect() throws Exception {

                    mockMvc.perform(get("/customers/" + customerIdByVat(ORG_XENIT_VAT) + "/invoices")
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(
                                    headers().location().uri("http://localhost/invoices?counterparty={id}", XENIT_ID));
                }
            }

            @Nested
            class OneToOne {

                @Test
                void getShippingAddress_forOrder_shouldReturn_http302_redirect() throws Exception {
                    mockMvc.perform(get("/orders/{id}/shippingAddress", ORDER_1_ID)
                                    .accept("application/json"))
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
                                    .accept("application/json"))
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

            }

            @Nested
            class OneToMany {

                @Test
                void putJson_shouldReplaceLinksAndReturn_http204_noContent() throws Exception {
                    var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                    var newOrderId = orders.save(new Order(xenit)).getId();
                    var invoiceNumber = invoiceId(INVOICE_NUMBER_1);

                    // try to add order to invoice using PUT - should fail
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
                    assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                        assertThat(invoice.getOrders()).singleElement().satisfies(order -> {
                            assertThat(order.getId()).isEqualTo(newOrderId);
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
            }

            @Nested
            class ManyToMany {

                @Test
                void putJson_emptyPromos_forOrder_shouldReturn_http204_noContent() throws Exception {

                    assertThat(orders.findById(ORDER_1_ID)).isNotNull()
                            .hasValueSatisfying(order -> assertThat(order.getPromos()).hasSize(1));

                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(""))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID)).isNotNull()
                            .hasValueSatisfying(order -> assertThat(order.getPromos()).hasSize(0));
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

                    var order = orders.findById(ORDER_1_ID).orElseThrow();
                    assertThat(order.getPromos()).hasSize(2);

                }

                @Test
                void putUriList_Promos_forOrder_shouldReturn_http204_noContent() throws Exception {
                    mockMvc.perform(put("/orders/{id}/promos", ORDER_1_ID)
                                    .accept("application/json")
                                    .contentType(RestMediaTypes.TEXT_URI_LIST_VALUE)
                                    .content("""
                                            /promotions/XMAS-2022
                                            /promotions/FREE-SHIP
                                            """)
                            )
                            .andExpect(status().isNoContent());

                    var order = orders.findById(ORDER_1_ID).orElseThrow();
                    assertThat(order.getPromos()).hasSize(2);
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
                void putJson_shouldAppend_http204_noContent() throws Exception {
                    var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                    var newOrderId = orders.save(new Order(xenit)).getId();

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
                    assertThat(invoices.findById(invoiceNumber)).hasValueSatisfying(invoice -> {
                        assertThat(invoice.getOrders())
                                .hasSize(3)
                                .anyMatch(order -> order.getId().equals(newOrderId));

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

                    var order = orders.findById(ORDER_1_ID).orElseThrow();
                    assertThat(order.getPromos()).hasSize(3);

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

                    var order = orders.findById(ORDER_1_ID).orElseThrow();
                    assertThat(order.getPromos()).hasSize(3);

                }
            }
        }

        @Nested
        @DisplayName("DELETE /{repository}/{id}/{property}")
        class Delete {

            @Nested
            class ManyToOne {

                @Test
                void deleteCounterparty_shouldReturn_http204() throws Exception {

                    // fictive example: dis-associate the customer from the invoice
                    // note that this would be classified as fraud in reality :grimacing:
                    mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/counterparty")
                                    .accept("application/json"))
                            .andExpect(status().isNoContent());

                    assertThat(invoices.findById(INVOICE_1_ID))
                            .hasValueSatisfying(invoice -> assertThat(invoice.getCounterparty()).isNull());
                }
            }

            @Nested
            class OneToMany {

                @Test
                void deleteToManyAssoc_shouldReturn_http405_methodNotAllowed() throws Exception {

                    // fictive example: dis-associate the customer from the invoice
                    // note that this would be classified as fraud in reality :grimacing:
                    mockMvc.perform(delete("/invoices/" + invoiceId(INVOICE_NUMBER_1) + "/orders")
                                    .accept("application/json"))
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
                                    .accept("application/json"))
                            .andExpect(status().isNoContent());

                    assertThat(orders.findById(ORDER_1_ID))
                            .hasValueSatisfying(order -> assertThat(order.getShippingAddress()).isNull());
                }
            }

            @Nested
            class ManyToMany {

                @Test
                void deletePromos_fromOrder_shouldReturn_http405_methodNotAllowed() throws Exception {
                    mockMvc.perform(delete("/orders/{orderId}/promos", ORDER_1_ID)
                                    .accept("application/json"))
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
                    var ordersIterable = orders.findAll(QOrder.order.invoice.number.eq(INVOICE_NUMBER_1));
                    var result = StreamSupport.stream(ordersIterable.spliterator(), false).toList();
                    assertThat(result).hasSize(2);

                    var firstOrderId = result.get(0).getId();

                    mockMvc.perform(get("/invoices/{invoice}/orders/{order}", invoiceId(INVOICE_NUMBER_1), firstOrderId)
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/orders/%s".formatted(firstOrderId))));
                }
            }

            @Nested
            class ManyToOne {

                @Test
                void getInvoiceCustomerById_shouldReturn_http302() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_NUMBER_1).orElseThrow();
                    var counterPartyId = invoice.getCounterparty().getId();

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + counterPartyId)
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/customers/%s".formatted(counterPartyId))));
                }

                @Test
                void getInvoiceCustomerByWrongId_shouldReturn_http404() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_NUMBER_1).orElseThrow();
                    var wrongCounterparty = customerIdByVat(ORG_INBEV_VAT);

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + wrongCounterparty)
                                    .accept("application/json"))
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
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(headers().location().uri("http://localhost/promotions/{promoCode}", PROMO_XMAS));

                }

                @Test
                void getPromoById_forOrder_invalidId_shouldReturn_http404_notFound() throws Exception {
                    mockMvc.perform(get("/orders/{id}/promos/{promoCode}", ORDER_1_ID, PROMO_XMAS)
                                    .accept("application/json"))
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
                    var invoice = invoices.findById(INVOICE_1_ID).orElseThrow();
                    assertThat(invoice.getOrders()).contains(orders.findById(ORDER_1_ID).orElseThrow());

                    mockMvc.perform(delete("/invoices/{invoice}/orders/{order}", INVOICE_1_ID, ORDER_1_ID)
                                    .accept("application/json"))
                            .andExpect(status().isNoContent());

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
                                            .accept("application/json"))
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
                                            .accept("application/json"))
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
        @DisplayName("GET /{repository}/{entityId}/{contentProperty}")
        class Get {

            @Test
            void getInvoiceContent() throws Exception {
                var filename = "💩 and 📝.txt";
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
                invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                invoice.setContentFilename(filename);
                invoices.save(invoice);

                var encodedFilename = UriUtils.encodeQuery(filename, StandardCharsets.UTF_8);
                mockMvc.perform(get("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                .accept(MediaType.ALL_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MIMETYPE_PLAINTEXT_UTF8))
                        .andExpect(content().string(EXT_ASCII_TEXT))
                        ;
                        /* This assertion is changed in SB3; and is technically incorrect
                        (it should be `Content-Disposition: attachment` or `Content-Disposition: inline` with a filename, never `form-data`)
                        .andExpect(headers().string("Content-Disposition",
                                is("form-data; name=\"attachment\"; filename*=UTF-8''%s".formatted(encodedFilename)))) */;
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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(UNICODE_TEXT);
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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("attachment"))).hasContent(
                        UNICODE_TEXT);
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
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                invoice.setContentFilename("content.txt");
                invoices.save(invoice);

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

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(
                        EXT_ASCII_TEXT);
                assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);

                // keeps original filename
                assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
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
            @Disabled("ACC-735")
            void postInvoiceContent_missingContentType_http400() throws Exception {
                mockMvc.perform(post("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                .content(UNICODE_TEXT))
                        .andExpect(status().isBadRequest());
            }

            @Test
            void postMultipartContent_http201() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                assertThat(invoicesContent.getContent(invoice)).isNull();

                var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                mockMvc.perform(multipart(HttpMethod.POST, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                .file(file))
                        .andExpect(status().isCreated());

                invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(UNICODE_TEXT);
                assertThat(invoice.getContentId()).isNotBlank();
                assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
            }

            @Test
            void postMultipartContent_updateDifferentContentType_http200() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
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

                invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                        .hasBinaryContent(content);
                assertThat(invoice.getContentId()).isNotBlank();
                assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                assertThat(invoice.getContentLength()).isEqualTo(image.contentLength());
                assertThat(invoice.getContentFilename()).isEqualTo("logo.png");
            }

            @Test
            void postMultipartContent_noPayload_http400() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                assertThat(invoicesContent.getContent(invoice)).isNull();

                mockMvc.perform(multipart(HttpMethod.POST, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isBadRequest());
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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(UNICODE_TEXT);
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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

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
                                .contentType("text/plain")
                                .content(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1)))
                        .andExpect(status().isCreated());

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

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

                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("attachment"))).hasContent(
                        UNICODE_TEXT);
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
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                var stream = new ByteArrayInputStream(EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1));
                invoicesContent.setContent(invoice, PropertyPath.from("content"), stream);
                invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                invoice.setContentFilename("content.txt");
                invoices.save(invoice);

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

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(
                        EXT_ASCII_TEXT);
                assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                assertThat(invoice.getContentLength()).isEqualTo(EXT_ASCII_TEXT_UTF8_LENGTH);

                // keeps original filename
                assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
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
            @Disabled("ACC-735")
            void putInvoiceContent_missingContentType_http400() throws Exception {
                mockMvc.perform(put("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                .content(UNICODE_TEXT))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @Disabled("ACC-735")
            void putMultipartContent_http201() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                assertThat(invoicesContent.getContent(invoice)).isNull();

                var bytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                var file = new MockMultipartFile("file", "content.txt", MIMETYPE_PLAINTEXT_UTF8, bytes);
                mockMvc.perform(multipart(HttpMethod.PUT, "/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1))
                                .file(file))
                        .andExpect(status().isCreated());

                invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content"))).hasContent(UNICODE_TEXT);
                assertThat(invoice.getContentId()).isNotBlank();
                assertThat(invoice.getContentMimetype()).isEqualTo(MIMETYPE_PLAINTEXT_UTF8);
                assertThat(invoice.getContentLength()).isEqualTo(UNICODE_TEXT_UTF8_LENGTH);
                assertThat(invoice.getContentFilename()).isEqualTo("content.txt");
            }

            @Test
            @Disabled("ACC-735")
            void putMultipartContent_updateDifferentContentType_http200() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
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

                invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));

                assertThat(invoicesContent.getContent(invoice, PropertyPath.from("content")))
                        .hasBinaryContent(content);
                assertThat(invoice.getContentId()).isNotBlank();
                assertThat(invoice.getContentMimetype()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                assertThat(invoice.getContentLength()).isEqualTo(image.contentLength());
                assertThat(invoice.getContentFilename()).isEqualTo("logo.png");
            }

            @Test
            void putMultipartContent_noPayload_http400() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
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
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                var bytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                invoicesContent.setContent(invoice, PropertyPath.from("content"), new ByteArrayInputStream(bytes));
                invoice.setContentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                invoice.setContentFilename("content.txt");
                invoices.save(invoice);

                mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());

                var content = invoicesContent.getResource(invoice, PropertyPath.from("content"));
                assertThat(content).isNull();
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
            void deleteMultipleContentProperties() throws Exception {
                var invoice = invoices.getReferenceById(invoiceId(INVOICE_NUMBER_1));
                var contentBytes = UNICODE_TEXT.getBytes(StandardCharsets.UTF_8);
                invoicesContent.setContent(invoice, PropertyPath.from("content"), new ByteArrayInputStream(contentBytes));
                invoice.setContentMimetype(MIMETYPE_PLAINTEXT_UTF8);
                invoice.setContentFilename("content.txt");

                var attachmentBytes = EXT_ASCII_TEXT.getBytes(StandardCharsets.ISO_8859_1);
                invoicesContent.setContent(invoice, PropertyPath.from("attachment"), new ByteArrayInputStream(attachmentBytes));
                invoice.setAttachmentMimetype(MIMETYPE_PLAINTEXT_LATIN1);
                invoice.setAttachmentFilename("attachment.txt");
                invoices.save(invoice);

                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNotNull();
                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNotNull();

                mockMvc.perform(delete("/invoices/{id}/attachment", invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());

                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNotNull();
                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNull();

                mockMvc.perform(delete("/invoices/{id}/content", invoiceId(INVOICE_NUMBER_1)))
                        .andExpect(status().isNoContent());

                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("content"))).isNull();
                assertThat(invoicesContent.getResource(invoice, PropertyPath.from("attachment"))).isNull();
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
