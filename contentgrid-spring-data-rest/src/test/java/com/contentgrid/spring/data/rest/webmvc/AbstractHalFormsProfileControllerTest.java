package com.contentgrid.spring.data.rest.webmvc;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// This class contains the tests for HAL Forms that should remain the same whether use-multipart-hal-forms is
// enabled or not. The HAL Forms that change when that property is enabled are in the subclasses of this class.
abstract class AbstractHalFormsProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void profileController_embeddedContent_searchForm() throws Exception {
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
                                "create-form": {}, # tested in subclasses
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
                                            name: "gender",
                                            type: "text"
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
    void profileController_requiredAssociation_searchForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/invoices")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                "create-form": {}, # tested in subclasses
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
    void profileController_noContent_createForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/orders")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                "create-form": {
                                    method: "POST",
                                    target: "http://localhost/orders",
                                    contentType: "application/json", # Always JSON, because there is no content property
                                    properties: [
                                        {
                                            name: "customer",
                                            type: "url"
                                        },
                                        {
                                            name: "shipping_address",
                                            type: "url"
                                        },
                                        # These many-to-many are still present because they are not ignored
                                        {
                                            name: "promos",
                                            type: "url"
                                        },
                                        {
                                            name: "manualPromos",
                                            type: "url"
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
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[0].name",
                        Matchers.is("vat")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[1].name",
                        Matchers.is("content.size")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[2].name",
                        Matchers.is("content.mimetype")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[3].name",
                        Matchers.is("content.filename")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[4].name",
                        Matchers.is("birthday")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[5].name",
                        Matchers.is("gender")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[6].name",
                        Matchers.is("invoices.number")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[7].name",
                        Matchers.is("invoices.paid")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[8].name",
                        Matchers.is("invoices.content.length")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[9].name",
                        Matchers.is("invoices.orders.id")));
    }
}