package com.contentgrid.spring.data.rest.webmvc;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
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
class HalFormsProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void profileController_embeddedContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                .accept(MediaTypes.HAL_FORMS_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk())
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
                                            name: "content.mimetype",
                                            type: "text"
                                        },
                                        {
                                            name: "content.filename",
                                            type: "text"
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
                                            name: "invoices.number",
                                            type: "text"
                                        },
                                        {
                                            name: "invoices.paid"
                                        # ,type: "checkbox"
                                        },
                                        # invoices.content.length does not run against the max depth limit because
                                        # they are static parameters on a field at depth 1
                                        {
                                            name: "invoices.content.length",
                                            type: "number"
                                        }
                                        # These have predicate=None set, so they are not present in the documentation
                                        #{
                                        #    name: "invoices.content.length.lt",
                                        #    type: "number"
                                        #},
                                        #{
                                        #    name: "invoices.content.length.gt",
                                        #    type: "number"
                                        #}
                                    ]
                                }
                            }
                        }
                        """));
    }

    @Test
    void profileController_requiredAssociation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/invoices")
                        .accept(MediaTypes.HAL_FORMS_JSON)
                ).andExpect(MockMvcResultMatchers.status().isOk())
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
                                        name: "content_mimetype",
                                        type: "text"
                                    },
                                    {
                                        name: "content_filename",
                                        type: "text"
                                    },
                                    {
                                        name: "attachment_mimetype",
                                        type: "text"
                                    },
                                    {
                                        name: "attachment_filename",
                                        type: "text"
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
                                    }
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
                                ]
                            }
                        }
                    }
                    """));
    }

}