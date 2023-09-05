package com.contentgrid.spring.data.rest.affordances;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class AffordanceInjectingSelfLinkProviderTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    Customer customer;

    @BeforeEach
    void setup() {
        customer = customerRepository.save(new Customer(UUID.randomUUID(), "Abc", "ABC", null, Set.of(), Set.of()));
    }

    @AfterEach
    void cleanup() {
        customerRepository.delete(customer);
        customer = null;
    }

    @Test
    void templatesAddedOnEntityInstance() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers/{id}", customer.getId())
                .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                self: {
                                    href: "http://localhost/customers/%s"
                                }
                            },
                            _templates: {
                                default: {
                                    method: "PUT",
                                    properties: [
                                        {
                                            name: "name",
                                            type: "text"
                                        },
                                        {
                                            name: "vat",
                                            type: "text"
                                        },
                                        {
                                            name: "content.mimetype",
                                            type: "text"
                                        },
                                        {
                                            name: "content.filename",
                                            type: "text"
                                        }
                                        # Note: no relations present, those are only for the create form
                                    ]
                                },
                                delete: {
                                    method: "DELETE"
                                }
                            }
                        }
                        """.formatted(customer.getId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.keys()", Matchers.containsInAnyOrder("default", "delete", "add-invoices", "add-orders")));
    }

    @Test
    void templatesAddedOnCollectionResource() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _embedded: {
                                "item": [
                                    {
                                        _links: {
                                            self: {
                                                href: "http://localhost/customers/%s"
                                            }
                                        },
                                        _templates: {
                                            default: {
                                                method: "PUT",
                                                properties: [
                                                    {
                                                        name: "name",
                                                        type: "text"
                                                    },
                                                    {
                                                        name: "vat",
                                                        type: "text"
                                                    },
                                                    {
                                                        name: "content.mimetype",
                                                        type: "text"
                                                    },
                                                    {
                                                        name: "content.filename",
                                                        type: "text"
                                                    }
                                                    # Note: no relations present, those are only for the create form
                                                ]
                                            },
                                            delete: {
                                                method: "DELETE"
                                            }
                                        }
                                    }
                                ]
                            }
                        }
                        """.formatted(customer.getId())))
                // No top-level _templates are present
                .andExpect(MockMvcResultMatchers.jsonPath("$.keys()", Matchers.not(Matchers.contains("_templates"))))
                // The templates of the embedded object only contain a default and a delete template
                .andExpect(MockMvcResultMatchers.jsonPath("$._embedded.['item'][0]._templates.keys()", Matchers.containsInAnyOrder("default", "delete", "add-invoices", "add-orders")));
        ;
    }

}