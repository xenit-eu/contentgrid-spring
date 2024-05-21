package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.security.WithMockJwt;
import org.hamcrest.Matchers;
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
@WithMockJwt
class HalFormsProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void profileController_embeddedContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                describes: [
                                    {
                                        name: "collection",
                                        href: "http://localhost/customers"
                                    },
                                    {
                                        name: "item",
                                        href: "http://localhost/customers/{id}",
                                        templated: true
                                    }
                                ]
                            },
                            _templates: {
                                "create-form": {
                                    method: "POST",
                                    contentType: "application/json",
                                    target: "http://localhost/customers",
                                    properties: [
                                        {
                                            name: "name",
                                            type: "text"
                                        },
                                        {
                                            name: "vat",
                                            required: true,
                                            type: "text"
                                        },
                                        {
                                            name: "birthday",
                                            type: "datetime"
                                        },
                                        {
                                            name: "total_spend",
                                            type: "number"
                                        },
                                        {
                                            name: "orders",
                                            type: "url",
                                            options: {
                                                valueField: "/_links/self/href",
                                                minItems: 0,
                                                link: {
                                                    href: "http://localhost/orders"
                                                }
                                            }
                                        },
                                        {
                                            name: "invoices",
                                            type: "url",
                                            options: {
                                                valueField: "/_links/self/href",
                                                minItems: 0,
                                                link: {
                                                    href: "http://localhost/invoices"
                                                }
                                            }
                                        }
                                    ]
                                },
                                search: {
                                    method: "GET",
                                    target: "http://localhost/customers",
                                    properties: [
                                        {
                                            name: "vat",
                                            type: "text"
                                        },
                                        {
                                            name: "content.size",
                                            type: "number"
                                        },
                                        {
                                            name: "content.mimetype",
                                            type: "text"
                                        },
                                        {
                                            name: "content.filename",
                                            type: "text"
                                        },
                                        {
                                            name: "birthday",
                                            type: "datetime"
                                        },
                                        {
                                            name: "invoices.number",
                                            type: "text"
                                        },
                                        {
                                            name: "invoices.paid",
                                            type: "checkbox"
                                        },
                                        # invoices.content.length does not run against the max depth limit because
                                        # they are static parameters on a field at depth 1
                                        {
                                            name: "invoices.content.length",
                                            type: "number"
                                        },
                                        # These have predicate=None set, so they are not present in the documentation
                                        #{
                                        #    name: "invoices.content.length.lt",
                                        #    type: "number"
                                        #},
                                        #{
                                        #    name: "invoices.content.length.gt",
                                        #    type: "number"
                                        #},
                                        # this is also a static parameter on a field at depth 1, so it is present
                                        {
                                            name: "invoices.orders.id"
                                        }
                                    ]
                                }
                            }
                        }
                        """));
    }

    @Test
    void profileController_requiredAssociation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/invoices")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                "create-form": {
                                    method: "POST",
                                    contentType: "application/json",
                                    target: "http://localhost/invoices",
                                    properties: [
                                        {
                                            name: "number",
                                            required: true,
                                            type: "text"
                                        },
                                        {
                                            name: "draft"
                                            # ,type: "checkbox"
                                        },
                                        {
                                            name: "paid"
                                            # ,type: "checkbox"
                                        },
                                        {
                                            name: "counterparty",
                                            type: "url",
                                            required: true,
                                            options: {
                                                valueField: "/_links/self/href",
                                                minItems: 1,
                                                maxItems: 1,
                                                link: {
                                                    href: "http://localhost/customers"
                                                }
                                            }
                                        },
                                        {
                                            name: "refund",
                                            type: "url",
                                            options: {
                                                valueField: "/_links/self/href",
                                                minItems: 0,
                                                maxItems: 1,
                                                link: {
                                                    href: "http://localhost/refunds"
                                                }
                                            }
                                        },
                                        {
                                            name: "orders",
                                            type: "url",
                                            options: {
                                                valueField: "/_links/self/href",
                                                minItems: 0,
                                                link: {
                                                    href: "http://localhost/orders"
                                                }
                                            }
                                        }
                                    ]
                                },
                                search: {
                                    method: "GET",
                                    target: "http://localhost/invoices",
                                    properties: [
                                        {
                                            name: "number",
                                            type: "text"
                                        },
                                        {
                                            name: "paid"
                                            # ,type: "checkbox"
                                        },
                                        {
                                            name: "content.length",
                                            type: "number"
                                        },
                                        # These have predicate=None set, so they are not present in the documentation
                                        #{
                                        #    name: "content.length.lt",
                                        #    type: "number"
                                        #},
                                        #{
                                        #    name: "content.length.gt",
                                        #    type: "number"
                                        #}
                                        # Note: no relation to orders is exposed,
                                        # because the searchable properties on Order are
                                        # also relations. We only expand one level deep
                                        # orders.id itself is exposed directly on invoice itself, so it is present
                                        {
                                            name: "orders.id"
                                        }
                                    ]
                                }
                            }
                        }
                        """));
    }

    @Test
    void profileController_orderedSearchParams() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[0].name", Matchers.is("vat")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[1].name",
                        Matchers.is("content.size")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[2].name",
                        Matchers.is("content.mimetype")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[3].name",
                        Matchers.is("content.filename")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[4].name",
                        Matchers.is("birthday")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[5].name",
                        Matchers.is("invoices.number")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[6].name",
                        Matchers.is("invoices.paid")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[7].name",
                        Matchers.is("invoices.content.length")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[8].name",
                        Matchers.is("invoices.orders.id")));
    }
}