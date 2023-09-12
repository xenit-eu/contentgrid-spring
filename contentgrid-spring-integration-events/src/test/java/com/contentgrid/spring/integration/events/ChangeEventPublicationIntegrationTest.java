package com.contentgrid.spring.integration.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.integration.events.TestConfig.TestMessageHandler;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

@SpringBootTest(classes = {InvoicingApplication.class, TestConfig.class})
public class ChangeEventPublicationIntegrationTest {

    @Autowired
    private TestMessageHandler testMessageHandler;

    @Autowired
    private CustomerRepository customerRepository;

    private static final String CUSTOMER_PAYLOAD = """
            {
                name: null,
                vat: "${#customerVat}",
                content: null,
                _links: {
                    self: {
                        href: "http://localhost/customers/${#customerId}"
                    },
                    "d:customer": {
                        href: "http://localhost/customers/${#customerId}"
                    },
                    "d:content": {
                        href: "http://localhost/customers/${#customerId}/content"
                    },
                    "d:invoices": {
                        href: "http://localhost/customers/${#customerId}/invoices"
                    },
                    "d:orders": {
                        href: "http://localhost/customers/${#customerId}/orders"
                    },
                    "cg:relation": [
                        {
                            name: "invoices",
                            href: "http://localhost/customers/${#customerId}/invoices"
                        },
                        {
                            name: "orders",
                            href: "http://localhost/customers/${#customerId}/orders"
                        }
                    ],
                    "cg:content": {
                        name: "content",
                        href: "http://localhost/customers/${#customerId}/content"
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
            """;

    private static String template(String template, Map<String, Object> variables) {
        var parser = new SpelExpressionParser();

        var context = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
        variables.forEach(context::setVariable);

        var expression = parser.parseExpression(template, new TemplateParserContext("${", "}"));

        return expression.getValue(context, String.class);
    }

    @AfterEach
    void cleanup() {
        customerRepository.deleteAll();
    }

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
                JSONAssert.assertEquals(template("""
                        {
                            trigger: "create",
                            old: null,
                            new: """+CUSTOMER_PAYLOAD+"""
                        }
                        """, Map.of("customerId", customer.getId(), "customerVat", "BE123")), body, false);
            });

        });

    }


    @Test
    void entityUpdate_emitsEvent() {
        var originalCustomer = new Customer();
        originalCustomer.setVat("BE123");
        var customer = customerRepository.save(originalCustomer);
        customer.setVat("BE456");

        customerRepository.save(customer);

        assertThat(testMessageHandler.lastMessage()).hasValueSatisfying(message -> {
            assertThat(message.getHeaders()).containsAllEntriesOf(Map.of(
                    "entity", "com.contentgrid.spring.test.fixture.invoicing.model.Customer",
                    "trigger", "update"
            ));

            assertThat(message.getPayload()).asString().satisfies(body -> {
                JSONAssert.assertEquals("""
                        {
                            trigger: "update",
                            old: """+template(CUSTOMER_PAYLOAD, Map.of("customerId", customer.getId(), "customerVat", "BE123"))+"""
                            ,
                            new: """+template(CUSTOMER_PAYLOAD, Map.of("customerId", customer.getId(), "customerVat", "BE456"))+"""
                        }
                        """, body, false);
            });

        });

    }

    @Test
    void entityDelete_emitsEvent() {
        var originalCustomer = new Customer();
        originalCustomer.setVat("BE123");
        var customer = customerRepository.save(originalCustomer);

        customerRepository.delete(customer);

        assertThat(testMessageHandler.lastMessage()).hasValueSatisfying(message -> {
            assertThat(message.getHeaders()).containsAllEntriesOf(Map.of(
                    "entity", "com.contentgrid.spring.test.fixture.invoicing.model.Customer",
                    "trigger", "delete"
            ));

            assertThat(message.getPayload()).asString().satisfies(body -> {
                JSONAssert.assertEquals("""
                        {
                            trigger: "delete",
                            old: """+template(CUSTOMER_PAYLOAD, Map.of("customerId", customer.getId(), "customerVat", "BE123"))+"""
                            ,
                            new: null
                        }
                        """, body, false);
            });

        });

    }
}
