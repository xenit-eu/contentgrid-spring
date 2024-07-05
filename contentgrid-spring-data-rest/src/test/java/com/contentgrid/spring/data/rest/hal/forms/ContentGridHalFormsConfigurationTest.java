package com.contentgrid.spring.data.rest.hal.forms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.security.WithMockJwt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
class ContentGridHalFormsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customers;

    @AfterEach
    void cleanUp() {
        customers.deleteAll();
    }

    @Test
    void createFormFieldForToOneRelationLinksToReferredResource() throws Exception {
        mockMvc.perform(get("/profile/refunds").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpectAll(
                        content().json("""
                                {
                                    _templates: {
                                        "create-form": {
                                            properties: [
                                                {
                                                    name: "invoice",
                                                    required: true,
                                                    type: "url",
                                                    options: {
                                                        link: {
                                                            href: "http://localhost/invoices"
                                                        },
                                                        minItems: 1,
                                                        maxItems: 1
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                }
                                """)
                );
    }

    @Test
    void createFormFieldForConstrainedAttributeOptions() throws Exception {
        mockMvc.perform(get("/profile/customers").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(content().json("""
                            {
                                _templates: {
                                    "create-form": {
                                        properties: [
                                            {
                                                name: "gender",
                                                type: "text",
                                                options: {
                                                    inline: [ "female", "male" ]
                                                }
                                            },
                                            {},{},{},{},{},{}
                                        ]
                                    }
                                }
                            }
                        """));
    }

    @Test
    void linkRelationFormFieldForToManyRelationLinksToReferredResource() throws Exception {
        var createdCustomer = mockMvc.perform(
                        post("/customers").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                    "vat": "BE123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(createdCustomer).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(
                content().json("""
                        {
                            _templates: {
                                "add-orders": {
                                    method: "POST",
                                    contentType: "text/uri-list",
                                    properties: [
                                        {
                                            name: "orders",
                                            type: "url",
                                            options: {
                                                link: {
                                                    href: "http://localhost/orders"
                                                },
                                                minItems: 0
                                            }
                                        }
                                    ]
                                },
                                "add-invoices": {
                                    method: "POST",
                                    contentType: "text/uri-list",
                                    properties: [
                                        {
                                            name: "invoices",
                                            type: "url",
                                            options: {
                                                link: {
                                                    href: "http://localhost/invoices"
                                                },
                                                minItems: 0
                                            }
                                        }
                                    ]
                                }
                            }
                        }
                        """)
        );
    }

    @Test
    void updateFormFieldForConstrainedAttributeOptions() throws Exception {
        var createdCustomer = mockMvc.perform(
                        post("/customers").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                    "vat": "BE123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(createdCustomer).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(
                content().json("""
                        {
                            _templates: {
                                default: {
                                    method: "PUT",
                                    properties: [
                                        {
                                            name: "gender",
                                            type: "text",
                                            options: {
                                                inline: [ "female", "male" ]
                                            }
                                        },
                                        {},{},{},{},{},{}
                                    ]
                                }
                            }
                        }
                        """)
        );
    }

}