package com.contentgrid.spring.integration.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.integration.events.ChangeEventPublicationIntegrationTest.TestConfig;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

@SpringBootTest(classes = {InvoicingApplication.class, TestConfig.class})
public class ChangeEventPublicationIntegrationTest {

    private static class TestMessageHandler extends AbstractMessageHandler {

        private final Deque<Message<?>> messages = new LinkedList<>();

        @Override
        public void destroy() {
            super.destroy();
            messages.clear();
        }

        @Override
        protected void handleMessageInternal(Message<?> message) {
            messages.push(message);
        }

        public Stream<Message<?>> messages() {
            return messages.stream();
        }

        public Optional<Message<?>> lastMessage() {
            return Optional.ofNullable(messages.getLast());
        }
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        TestMessageHandler mockMessageHandler() {
            return new TestMessageHandler();
        }

        @Bean
        EntityChangeEventHandler testEventHandler(TestMessageHandler testMessageHandler) {
            return () -> testMessageHandler;
        }
    }

    @Autowired
    private TestMessageHandler testMessageHandler;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void entityCreate_emitsEvent() {
        var toCreate = new Customer();
        toCreate.setId(UUID.randomUUID());
        toCreate.setVat("BE123");
        var customer = customerRepository.save(toCreate);

        assertThat(testMessageHandler.lastMessage()).hasValueSatisfying(message -> {
            assertThat(message.getHeaders()).containsAllEntriesOf(Map.of(
                    "entity", "com.contentgrid.spring.test.fixture.invoicing.model.Customer",
                    "trigger", "create"
            ));

            assertThat(message.getPayload()).asString().satisfies(body -> {
                JSONAssert.assertEquals("""
                        {
                            trigger: "create",
                            old: null,
                            new: {
                                name: null,
                                vat: "BE123",
                                content: null,
                                _links: {
                                    self: {
                                        href: "http://localhost/customers/{customerId}"
                                    },
                                    "d:customer": {
                                        href: "http://localhost/customers/{customerId}"
                                    },
                                    "d:content": {
                                        href: "http://localhost/customers/{customerId}/content"
                                    },
                                    "d:invoices": {
                                        href: "http://localhost/customers/{customerId}/invoices"
                                    },
                                    "d:orders": {
                                        href: "http://localhost/customers/{customerId}/orders"
                                    },
                                    "cg:relation": [
                                        {
                                            name: "invoices",
                                            href: "http://localhost/customers/{customerId}/invoices"
                                        },
                                        {
                                            name: "orders",
                                            href: "http://localhost/customers/{customerId}/orders"
                                        }
                                    ],
                                    "cg:content": {
                                        name: "content",
                                        href: "http://localhost/customers/{customerId}/content"
                                    },
                                    "curies": [
                                        {
                                            name: "d",
                                            templated: true
                                        },
                                        {
                                            name: "cg",
                                            templated: true
                                        }
                                    ]
                                }
                            }
                        }
                        """.replaceAll("\\{customerId}", customer.getId().toString()), body, false);
            });

        });

    }


}
