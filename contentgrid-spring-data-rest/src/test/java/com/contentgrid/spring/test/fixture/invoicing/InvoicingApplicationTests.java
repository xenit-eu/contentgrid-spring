package com.contentgrid.spring.test.fixture.invoicing;

import static com.contentgrid.spring.test.matchers.ExtendedHeaderResultMatchers.headers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

@Slf4j
@Transactional
@SpringBootTest
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

    @BeforeEach
    void setupTestData() {
        PROMO_XMAS = promos.save(new PromotionCampaign("XMAS-2022", "10% off ")).getPromoCode();
        PROMO_SHIPPING = promos.save(new PromotionCampaign("FREE-SHIP", "Free Shipping")).getPromoCode();
        var promoCyber = promos.save(new PromotionCampaign("CYBER-MON", "Cyber Monday"));
        PROMO_CYBER = promoCyber.getPromoCode();

        var xenit = customers.save(new Customer(null, "XeniT", ORG_XENIT_VAT, new HashSet<>(), new HashSet<>()));
        var inbev = customers.save(new Customer(null, "AB InBev", ORG_INBEV_VAT, new HashSet<>(), new HashSet<>()));

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
                        .andExpect(jsonPath("$._embedded.invoices.length()").value(2));
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
                                                "promos" : [
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

    private UUID invoiceId(String number) {
        return invoices.findByNumber(number).map(Invoice::getId).orElseThrow();
    }

    private UUID customerIdByVat(String vat) {
        return customers.findByVat(vat).map(Customer::getId).orElseThrow();
    }


}
