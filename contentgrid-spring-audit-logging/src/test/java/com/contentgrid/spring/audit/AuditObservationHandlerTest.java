package com.contentgrid.spring.audit;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.contentgrid.spring.audit.event.AbstractAuditEvent;
import com.contentgrid.spring.audit.event.AbstractAuditEvent.AbstractAuditEventBuilder;
import com.contentgrid.spring.audit.event.EntityContentAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent;
import com.contentgrid.spring.audit.event.EntityItemAuditEvent.Operation;
import com.contentgrid.spring.audit.event.EntityRelationAuditEvent;
import com.contentgrid.spring.audit.event.EntityRelationItemAuditEvent;
import com.contentgrid.spring.audit.event.EntitySearchAuditEvent;
import com.contentgrid.spring.audit.extractor.AuditEventExtractor;
import com.contentgrid.spring.audit.extractor.BasicAuditEventExtractor;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import com.contentgrid.spring.audit.test.handler.AggregatingAuditHandler;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.OrderRepository;
import com.contentgrid.spring.test.fixture.invoicing.store.CustomerContentStore;
import com.contentgrid.spring.test.security.WithMockJwt;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;

@SpringBootTest(
        classes = {
                InvoicingApplication.class,
                AuditObservationHandlerTest.TestConfig.class
        },
        properties = {
                "server.servlet.encoding.enabled=false", // disables mock-mvc enforcing charset in request
                "contentgrid.audit.messaging.enabled=false"
        }
)
@AutoConfigureMockMvc
@WithMockJwt
class AuditObservationHandlerTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        AggregatingAuditHandler aggregatingAuditHandler() {
            return new AggregatingAuditHandler();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CustomerContentStore customerContentStore;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    AggregatingAuditHandler auditHandler;

    UUID CUSTOMER_ID;

    UUID INVOICE_ID;

    @BeforeEach
    void createItem() {
        customerRepository.findByVat("abc")
                .ifPresentOrElse(customer -> {
                    CUSTOMER_ID = customer.getId();
                }, () -> {
                    var customer = new Customer();
                    customer.setName("test");
                    customer.setVat("abc");
                    CUSTOMER_ID = customerRepository.save(customer).getId();
                });

        invoiceRepository.findByNumber("123")
                .ifPresentOrElse(invoice -> {
                    INVOICE_ID = invoice.getId();
                }, () -> {
                    var invoice = new Invoice();
                    invoice.setNumber("123");
                    invoice.setCounterparty(customerRepository.findByVat("abc").orElseThrow());
                    INVOICE_ID = invoiceRepository.save(invoice).getId();
                });

        var customer = customerRepository.findByVat("abc").orElseThrow();
        customerContentStore.setContent(
                customer,
                PropertyPath.from("content"),
                new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8))
        );
        customerRepository.save(customer);
    }

    @BeforeEach
    void clearEvents() {
        auditHandler.getEvents().clear();
    }

    static Stream<MediaType> mediaTypesAll() {
        return Stream.of(
                MediaType.APPLICATION_JSON,
                MediaTypes.HAL_JSON,
                MediaTypes.HAL_FORMS_JSON
        );
    }

    static Stream<MediaType> mediaTypesCollection() {
        return Stream.of(
                RestMediaTypes.SPRING_DATA_COMPACT_JSON,
                RestMediaTypes.TEXT_URI_LIST
        );
    }

    @ParameterizedTest
    @MethodSource({"mediaTypesAll", "mediaTypesCollection"})
    void collection(MediaType mediaType) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers")
                .accept(mediaType)
        ).andExpect(MockMvcResultMatchers.status().isOk());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntitySearchAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getQueryParameters()).isEmpty();
                });
    }

    @Test
    void itemCreate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "test customer1",
                            "vat": "1234"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isCreated());

        var created = customerRepository.findByVat("1234").orElseThrow();

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(Operation.CREATE);
                    assertThat(event.getId()).isEqualTo(created.getId().toString());
                });
    }

    @ParameterizedTest
    @MethodSource("mediaTypesAll")
    void itemGet(MediaType mediaType) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers/{id}", CUSTOMER_ID)
                .accept(mediaType)
        ).andExpect(MockMvcResultMatchers.status().isOk());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(Operation.READ);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                });
    }

    @Test
    void itemUpdatePut() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/customers/{id}", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "zzz",
                            "vat": "888"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(Operation.UPDATE);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                });
    }

    @Test
    void itemUpdatePatch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/customers/{id}", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "zzz"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(Operation.UPDATE);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                });
    }

    @Test
    void itemDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/invoices/{id}", INVOICE_ID))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Invoice.class);
                    assertThat(event.getOperation()).isEqualTo(Operation.DELETE);
                    assertThat(event.getId()).isEqualTo(INVOICE_ID.toString());
                });
    }

    @ParameterizedTest
    @MethodSource("mediaTypesAll")
    void relationGet(MediaType mediaType) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers/{id}/orders", CUSTOMER_ID)
                .accept(mediaType)
        ).andExpect(MockMvcResultMatchers.status().is3xxRedirection());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationAuditEvent.Operation.READ);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("orders");
                });
    }

    @Test
    void relationPost() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/customers/{id}/invoices", CUSTOMER_ID)
                .contentType(RestMediaTypes.TEXT_URI_LIST)
                .content("""
                        /invoices/%s
                        """.formatted(INVOICE_ID))
        ).andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationAuditEvent.Operation.UPDATE);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("invoices");
                });
    }

    @Test
    void relationPut() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/invoices/{id}/counterparty", INVOICE_ID)
                .contentType(RestMediaTypes.TEXT_URI_LIST)
                .content("""
                        /customers/%s
                        """.formatted(CUSTOMER_ID))
        ).andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Invoice.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationAuditEvent.Operation.UPDATE);
                    assertThat(event.getId()).isEqualTo(INVOICE_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("counterparty");
                });
    }

    @Test
    void relationDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/invoices/{id}/refund", INVOICE_ID))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Invoice.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationAuditEvent.Operation.DELETE);
                    assertThat(event.getId()).isEqualTo(INVOICE_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("refund");
                });
    }

    @ParameterizedTest
    @MethodSource("mediaTypesAll")
    void relationGetItem(MediaType mediaType) throws Exception {
        var order = new Order();
        order.setCustomer(customerRepository.getReferenceById(CUSTOMER_ID));
        var savedOrder = orderRepository.save(order);
        var invoice = invoiceRepository.findById(INVOICE_ID).orElseThrow();
        invoice.setOrders(Set.of(savedOrder));
        invoiceRepository.save(invoice);
        mockMvc.perform(MockMvcRequestBuilders.get("/invoices/{id}/orders/{id}", INVOICE_ID, savedOrder.getId())
                        .accept(mediaType)
                )
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Invoice.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationItemAuditEvent.Operation.READ);
                    assertThat(event.getId()).isEqualTo(INVOICE_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("orders");
                    assertThat(event.getRelationId()).isEqualTo(savedOrder.getId().toString());
                });
    }

    @Test
    void relationDeleteItem() throws Exception {
        var orderId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete("/invoices/{id}/orders/{id}", INVOICE_ID, orderId))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityRelationItemAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Invoice.class);
                    assertThat(event.getOperation()).isEqualTo(EntityRelationItemAuditEvent.Operation.DELETE);
                    assertThat(event.getId()).isEqualTo(INVOICE_ID.toString());
                    assertThat(event.getRelationName()).isEqualTo("orders");
                    assertThat(event.getRelationId()).isEqualTo(orderId.toString());
                });
    }

    @Test
    void contentGet() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers/{id}/content", CUSTOMER_ID))
                .andExpect(MockMvcResultMatchers.status().isOk());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityContentAuditEvent.class, event -> {
                    assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    assertThat(event.getContentName()).isEqualTo("content");
                    assertThat(event.getOperation()).isEqualTo(EntityContentAuditEvent.Operation.READ);
                });
    }

    @ParameterizedTest
    @CsvSource({"PUT", "POST"})
    void contentSetDirect(HttpMethod method) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(method, "/customers/{id}/content", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("test123")
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityContentAuditEvent.class, event -> {
                    // TODO: extract domain type, id and content name
                    // assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    // assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    // assertThat(event.getContentName()).isEqualTo("content");
                    assertThat(event.getOperation()).isEqualTo(EntityContentAuditEvent.Operation.UPDATE);
                });

    }

    @ParameterizedTest
    @CsvSource({"PUT", "POST"})
    void contentSetMultipart(HttpMethod method) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart(method, "/customers/{id}/content", CUSTOMER_ID)
                        .file(new MockMultipartFile(
                                "file",
                                "test.txt",
                                "text/plain",
                                "abcdef".getBytes(StandardCharsets.UTF_8)
                        ))
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityContentAuditEvent.class, event -> {
                    // TODO: extract domain type, id and content name
                    // assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    // assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    // assertThat(event.getContentName()).isEqualTo("content");
                    assertThat(event.getOperation()).isEqualTo(EntityContentAuditEvent.Operation.UPDATE);
                });
    }

    @Test
    void contentDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/customers/{id}/content", CUSTOMER_ID))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(EntityContentAuditEvent.class, event -> {
                    // TODO: extract domain type, id and content name
                    // assertThat(event.getDomainType()).isEqualTo(Customer.class);
                    // assertThat(event.getId()).isEqualTo(CUSTOMER_ID.toString());
                    // assertThat(event.getContentName()).isEqualTo("content");
                    assertThat(event.getOperation()).isEqualTo(EntityContentAuditEvent.Operation.DELETE);
                });
    }

    @Nested
    class HandlesThrownExceptions {

        private static final ServerRequestObservationContext SERVER_REQUEST_OBSERVATION_CONTEXT = createContext();

        private static ServerRequestObservationContext createContext() {
            var request = new MockHttpServletRequest();
            request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                    (HandlerFunction) serverRequest -> ServerResponse.ok().build());
            return new ServerRequestObservationContext(request, new MockHttpServletResponse());
        }

        @Test
        void handlesExceptionsDuringExtraction() {
            var aggregating = new AggregatingAuditHandler();
            var handler = new AuditObservationHandler(
                    List.of(new ThrowingDuringCreateAuditEventExtractor(), new BasicAuditEventExtractor()),
                    List.of(aggregating)
            );

            assertThatCode(() -> {
                handler.onStop(SERVER_REQUEST_OBSERVATION_CONTEXT);
            }).doesNotThrowAnyException();

            assertThat(aggregating.getEvents()).isNotEmpty();
        }

        @Test
        void handlesExceptionsDuringEnhance() {
            var aggregating = new AggregatingAuditHandler();
            var handler = new AuditObservationHandler(
                    List.of(new ThrowingDuringEnhanceAuditEventExtractor(), new BasicAuditEventExtractor()),
                    List.of(aggregating)
            );

            assertThatCode(() -> {
                handler.onStop(SERVER_REQUEST_OBSERVATION_CONTEXT);
            }).doesNotThrowAnyException();

            assertThat(aggregating.getEvents()).isNotEmpty();
        }

        @Test
        void handlesExceptionsDuringHandle() {
            var aggregating = new AggregatingAuditHandler();
            var handler = new AuditObservationHandler(
                    List.of(new BasicAuditEventExtractor()),
                    List.of(new ThrowingAuditEventHandler(), aggregating)
            );

            assertThatCode(() -> {
                handler.onStop(SERVER_REQUEST_OBSERVATION_CONTEXT);
            }).doesNotThrowAnyException();

            assertThat(aggregating.getEvents()).isNotEmpty();
        }

        private static class ThrowingDuringCreateAuditEventExtractor implements AuditEventExtractor {

            @Override
            public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(
                    ServerRequestObservationContext context) {
                throw new RuntimeException("BOO!");
            }

            @Override
            public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
                    AbstractAuditEventBuilder<?, ?> eventBuilder) {
                return null;
            }
        }

        private static class ThrowingDuringEnhanceAuditEventExtractor implements AuditEventExtractor {

            @Override
            public Optional<AbstractAuditEventBuilder<?, ?>> createEventBuilder(
                    ServerRequestObservationContext context) {
                return Optional.empty();
            }

            @Override
            public AbstractAuditEventBuilder<?, ?> enhance(ServerRequestObservationContext context,
                    AbstractAuditEventBuilder<?, ?> eventBuilder) {
                throw new RuntimeException("BOO!");
            }
        }

        private static class ThrowingAuditEventHandler implements AuditEventHandler {

            @Override
            public void handle(AbstractAuditEvent auditEvent) {
                throw new RuntimeException("BOO!");
            }
        }
    }


    @ParameterizedTest
    @CsvSource({
            "GET,/",
            "GET,/profile/customers",
            "GET,/customers",
            "GET,/customers/abc",
            "GET,/customers/abc/content",
            "POST,/customers",
            "PUT,/customers/abc",
            "DELETE,/customers/abc",
            "PATCH,/customers/abc",
            "GET,/customers/abc/orders",
            "PUT,/customers/abc/orders",
            "PATCH,/customers/abc/orders",
            "DELETE,/customers/abc/orders",
    })
    void basicAuditEvents(HttpMethod method, String uri) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.request(method, uri))
                .andReturn();

        assertThat(auditHandler.getEvents()).singleElement()
                .isInstanceOfSatisfying(AbstractAuditEvent.class, event -> {
                    assertThat(event.getRequestMethod()).isEqualTo(method.toString());
                    assertThat(event.getRequestUri()).isEqualTo(uri);
                    assertThat(event.getResponseStatus()).isEqualTo(result.getResponse().getStatus());
                    if (result.getResponse().getRedirectedUrl() != null) {
                        assertThat(event.getResponseLocation()).isEqualTo(
                                result.getResponse().getRedirectedUrl().replace("http://localhost", ""));
                    } else {
                        assertThat(event.getResponseLocation()).isNull();
                    }
                });

    }

    @ParameterizedTest
    @CsvSource({
            // Static URLs
            "GET,/explorer",
            "GET,/explorer/index.html",
            "GET,/webjars/swagger-ui/index.html",
            "GET,/openapi.yml",
            // Unmapped URLs
            "DELETE,/customers",
            "PUT,/customers",
            "PATCH,/customers",
            "POST,/xyz",
            // Invalid methods
            "get,/",
            "XYZ,/customers",
            "xyz,/customers"
    })
    void noAuditEvents(HttpMethod method, String uri) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(method, uri))
                .andReturn();

        assertThat(auditHandler.getEvents()).isEmpty();
    }
}