package com.contentgrid.spring.test.fixture.invoicing;

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

import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.OrderRepository;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.QOrder;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ThunxDemoApplicationTests {

    static final String INVOICE_1 = "I-2022-0001";
    static final String INVOICE_2 = "I-2022-0002";

    static final String ORG_XENIT_VAT = "BE0887582365";
    static final String ORG_INBEV_VAT = "BE0417497106";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    CustomerRepository customers;

    @Autowired
    InvoiceRepository invoices;

    @Autowired
    OrderRepository orders;

    @BeforeEach
    void setupTestData() {
        // class is annotated with @Transactional, any change gets rolled back at the end of every test
        var xenit = customers.save(new Customer(null, "XeniT", ORG_XENIT_VAT, new HashSet<>(), new HashSet<>()));
        var inbev = customers.save(new Customer(null, "AB InBev", ORG_INBEV_VAT, new HashSet<>(), new HashSet<>()));

        var order1 = orders.save(new Order(xenit));
        var order2 = orders.save(new Order(xenit));
        var order3 = orders.save(new Order(inbev));

        invoices.saveAll(List.of(
                new Invoice(INVOICE_1, true, false, xenit, new HashSet<>(List.of(order1, order2))),
                new Invoice(INVOICE_2, false, true, inbev, new HashSet<>(List.of(order3)))
        ));
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
                        .andExpect(status().isCreated());
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

                mockMvc.perform(get("/invoices/" + invoiceIdByNumber(INVOICE_1))
                                .contentType("application/json"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.number").value(INVOICE_1));
            }

        }

        @Nested
        @DisplayName("HEAD /{repository}/{id}")
        class Head {

            @Test
            void headInvoice_shouldReturn_http204_noContent() throws Exception {
                mockMvc.perform(head("/invoices/" + invoiceIdByNumber(INVOICE_1))
                                .contentType("application/json"))
                        .andExpect(status().isNoContent());
            }


        }

        @Nested
        @DisplayName("PUT /{repository}/{id}")
        class Put {

            @Test
            void putInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(put("/invoices/" + invoiceIdByNumber(INVOICE_1))
                                .contentType("application/json")
                                .content("""
                                        {
                                            "number": "%s",
                                            "paid": true
                                        }
                                        """.formatted(INVOICE_1)))
                        .andExpect(status().isNoContent());
            }

        }

        @Nested
        @DisplayName("PATCH /{repository}/{id}")
        class Patch {

            @Test
            void patchInvoice_shouldReturn_http204_ok() throws Exception {
                mockMvc.perform(patch("/invoices/" + invoiceIdByNumber(INVOICE_1))
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
                mockMvc.perform(delete("/invoices/" + invoiceIdByNumber(INVOICE_1)))
                        .andExpect(status().isNoContent());

                assertThat(invoices.findByNumber(INVOICE_1)).isEmpty();
            }

        }
    }

    @Nested
    class AssociationResource {

        @Nested
        @DisplayName("GET /{repository}/{id}/{property}")
        class Get {

            @Nested
            class ToOne {

                @Test
                void getInvoiceCustomer_shouldReturn_http302_redirect() throws Exception {

                    mockMvc.perform(get("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/counterparty")
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/customers/" + customerIdByVat(ORG_XENIT_VAT))));
                }


            }

            @Nested
            class ToMany {

                @Test
                void getInvoicesForCustomer_shouldReturn_http302() throws Exception {

                    mockMvc.perform(get("/customers/" + customerIdByVat(ORG_XENIT_VAT) + "/invoices")
                                    .accept("application/json"))
                            .andExpect(status().isFound());
                }
            }
        }

        @Nested
        @DisplayName("PUT /{repository}/{id}/{property}")
        class Put {

            @Nested
            class ToOne {

                @Test
                void putJson_shouldReturn_http204() throws Exception {
                    // fictive example: fix the customer
                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    mockMvc.perform(put("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/counterparty")
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
            class ToMany {

                @Test
                void putJson_shouldReplaceLinksAndReturn_http204_noContent() throws Exception {
                    var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                    var newOrderId = orders.save(new Order(xenit)).getId();
                    var invoiceNumber = invoiceIdByNumber(INVOICE_1);

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
        }

        @Nested
        @DisplayName("POST /{repository}/{id}/{property}")
        class Post {

            @Nested
            class ToOne {

                @Test
                void postJson_shouldReturn_http405_methodNotAllowed() throws Exception {

                    var correctCustomerId = customers.findByVat(ORG_INBEV_VAT).orElseThrow().getId();
                    mockMvc.perform(post("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/counterparty")
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
            class ToMany {

                @Test
                void putJson_shouldAppend_http204_noContent() throws Exception {
                    var xenit = customers.findByVat(ORG_XENIT_VAT).orElseThrow();
                    var newOrderId = orders.save(new Order(xenit)).getId();

                    var invoiceNumber = invoiceIdByNumber(INVOICE_1);

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

        }

        @Nested
        @DisplayName("DELETE /{repository}/{id}/{property}")
        class Delete {

            @Nested
            class ToOne {

                @Test
                void deleteToOneAssoc_shouldReturn_http204() throws Exception {

                    // fictive example: dis-associate the customer from the invoice
                    // note that this would be classified as fraud in reality :grimacing:
                    mockMvc.perform(delete("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/counterparty")
                                    .accept("application/json"))
                            .andExpect(status().isNoContent());
                }
            }

            @Nested
            class ToMany {

                @Test
                void deleteToManyAssoc_shouldReturn_http405_methodNotAllowed() throws Exception {

                    // fictive example: dis-associate the customer from the invoice
                    // note that this would be classified as fraud in reality :grimacing:
                    mockMvc.perform(delete("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/orders")
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
            class ToMany {

                @Test
                void getInvoicesOrders_shouldReturn_http302() throws Exception {
                    var ordersIterable = orders.findAll(QOrder.order.invoice.number.eq(INVOICE_1));
                    var result = StreamSupport.stream(ordersIterable.spliterator(), false).toList();
                    assertThat(result).hasSize(2);

                    var firstOrderId = result.get(0).getId();

                    mockMvc.perform(get("/invoices/" + invoiceIdByNumber(INVOICE_1) + "/orders/" + firstOrderId)
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/orders/%s".formatted(firstOrderId))));
                }
            }

            @Nested
            class ToOne {

                @Test
                void getInvoiceCustomerById_shouldReturn_http302() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_1).orElseThrow();
                    var counterPartyId = invoice.getCounterparty().getId();

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + counterPartyId)
                                    .accept("application/json"))
                            .andExpect(status().isFound())
                            .andExpect(header().string(HttpHeaders.LOCATION,
                                    endsWith("/customers/%s".formatted(counterPartyId))));
                }

                @Test
                void getInvoiceCustomerByWrongId_shouldReturn_http404() throws Exception {
                    var invoice = invoices.findByNumber(INVOICE_1).orElseThrow();
                    var wrongCounterparty = customerIdByVat(ORG_INBEV_VAT);

                    mockMvc.perform(get("/invoices/" + invoice.getId() + "/counterparty/" + wrongCounterparty)
                                    .accept("application/json"))
                            .andExpect(status().isNotFound());
                }
            }
        }
    }

    private UUID invoiceIdByNumber(String number) {
        return invoices.findByNumber(number).map(Invoice::getId).orElseThrow();
    }

    private UUID customerIdByVat(String vat) {
        return customers.findByVat(vat).map(Customer::getId).orElseThrow();
    }
}
