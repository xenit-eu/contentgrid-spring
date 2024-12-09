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
                                        },
                                        {
                                            name: "sort"
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
                                        },
                                        {
                                            name: "sort",
                                            options: {
                                                minItems: 0,
                                                inline: [
                                                    {
                                                        value: "number,asc",
                                                        property: "number",
                                                        direction: "asc"
                                                    },
                                                    {
                                                        value: "number,desc",
                                                        property: "number",
                                                        direction: "desc"
                                                    },
                                                    {
                                                        value: "paid,asc",
                                                        property: "paid",
                                                        direction: "asc"
                                                    },
                                                    {
                                                        value: "paid,desc",
                                                        property: "paid",
                                                        direction: "desc"
                                                    },
                                                    {
                                                        value: "content.length,asc",
                                                        property: "content.length",
                                                        direction: "asc"
                                                    },
                                                    {
                                                        value: "content.length,desc",
                                                        property: "content.length",
                                                        direction: "desc"
                                                    }
                                                ]
                                            }
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
                        Matchers.is("invoices.orders.id")))
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties[10].name",
                        Matchers.is("sort")));
    }

    @Test
    void profileController_entityInformation_withEmbeddedContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            name: "customer",
                            title: "Client",
                            description: "Company or person that acts like a client",
                            _embedded: {
                                "blueprint:attribute": [
                                    {
                                        name: "id",
                                        type: "string",
                                        readOnly: true
                                    },
                                    {
                                        name: "audit_metadata",
                                        type: "object",
                                        readOnly: true,
                                        _embedded: {
                                            "blueprint:attribute": [
                                                {
                                                    name: "created_by",
                                                    type: "string",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "created_date",
                                                    type: "datetime",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "last_modified_by",
                                                    type: "string",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "last_modified_date",
                                                    type: "datetime",
                                                    readOnly: true
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "name",
                                        title: "Customer name",
                                        type: "string",
                                        description: "Full name of the customer"
                                    },
                                    {
                                        name: "vat",
                                        title: "VAT number",
                                        type: "string",
                                        description: "VAT number of the customer",
                                        required: true,
                                        _embedded: {
                                            "blueprint:constraint": [
                                                {
                                                    type: required
                                                },
                                                {
                                                    type: unique
                                                }
                                            ],
                                            "blueprint:search-param": [
                                                {
                                                    name: "vat",
                                                    type: "case-insensitive-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "content",
                                        type: "object",
                                        _embedded: {
                                            "blueprint:attribute": [
                                                {
                                                    name: "length",
                                                    type: "long",
                                                    readOnly: true,
                                                    _embedded: {
                                                        "blueprint:search-param": [
                                                            {
                                                                name: "content.size",
                                                                type: "exact-match"
                                                            }
                                                        ]
                                                    }
                                                },
                                                {
                                                    name: "mimetype",
                                                    type: "string",
                                                    _embedded: {
                                                        "blueprint:search-param": [
                                                            {
                                                                name: "content.mimetype",
                                                                type: "exact-match"
                                                            }
                                                        ]
                                                    }
                                                },
                                                {
                                                    name: "filename",
                                                    type: "string",
                                                    _embedded: {
                                                        "blueprint:search-param": [
                                                            {
                                                                name: "content.filename",
                                                                type: "exact-match"
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "birthday",
                                        type: "datetime",
                                        _embedded: {
                                            "blueprint:search-param": [
                                                {
                                                    name: "birthday",
                                                    type: "exact-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "gender",
                                        type: "string",
                                        _embedded: {
                                            "blueprint:constraint": [
                                                {
                                                    type: "allowed-values",
                                                    values: ["female", "male"]
                                                }
                                            ],
                                            "blueprint:search-param": [
                                                {
                                                    name: "gender",
                                                    type: "exact-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "total_spend",
                                        title: "Total Amount Spent",
                                        type: "long",
                                        description: "Total amount of money spent (in euros)"
                                    }
                                ],
                                "blueprint:relation": [
                                    {
                                        name: "orders",
                                        many_source_per_target: false,
                                        many_target_per_source: true,
                                        _links: {
                                            "blueprint:target-entity": {
                                                href: "http://localhost/profile/orders"
                                            }
                                        }
                                    },
                                    {
                                        name: "invoices",
                                        many_source_per_target: false,
                                        many_target_per_source: true,
                                        _links: {
                                            "blueprint:target-entity": {
                                                href: "http://localhost/profile/invoices"
                                            }
                                        }
                                    }
                                ]
                            }
                        }
                        """));
    }

    @Test
    void profileController_entityInformation_withInlineContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/invoices").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            name: "invoice",
                            title: "Invoice",
                            description: "A bill containing a counterparty and several orders",
                            _embedded: {
                                "blueprint:attribute": [
                                    {
                                        name: "id",
                                        type: "string",
                                        readOnly: true
                                    },
                                    {
                                        name: "audit_metadata",
                                        type: "object",
                                        readOnly: true,
                                        _embedded: {
                                            "blueprint:attribute": [
                                                {
                                                    name: "created_by",
                                                    type: "string",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "created_date",
                                                    type: "datetime",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "last_modified_by",
                                                    type: "string",
                                                    readOnly: true
                                                },
                                                {
                                                    name: "last_modified_date",
                                                    type: "datetime",
                                                    readOnly: true
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "number",
                                        type: "string",
                                        description: "Identifier of the invoice",
                                        required: true,
                                        _embedded: {
                                            "blueprint:constraint": [
                                                {
                                                    type: "required"
                                                }
                                            ],
                                            "blueprint:search-param": [
                                                {
                                                    name: "number",
                                                    type: "case-insensitive-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "draft",
                                        type: "boolean"
                                    },
                                    {
                                        name: "paid",
                                        type: "boolean",
                                        _embedded: {
                                            "blueprint:search-param": [
                                                {
                                                    name: "paid",
                                                    type: "exact-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "content_length",
                                        type: "long",
                                        readOnly: true,
                                        _embedded: {
                                            "blueprint:search-param": [
                                                {
                                                    name: "content.length",
                                                    type: "exact-match"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        name: "content_mimetype",
                                        type: "string"
                                    },
                                    {
                                        name: "content_filename",
                                        type: "string"
                                    },
                                    {
                                        name: "attachment_length",
                                        type: "long",
                                        readOnly: true
                                    },
                                    {
                                        name: "attachment_mimetype",
                                        type: "string"
                                    },
                                    {
                                        name: "attachment_filename",
                                        type: "string"
                                    }
                                ],
                                "blueprint:relation": [
                                    {
                                        name: "counterparty",
                                        description: "Client that has to pay the invoice",
                                        many_source_per_target: true,
                                        many_target_per_source: false,
                                        required: true,
                                        _links: {
                                            "blueprint:target-entity": {
                                                href: "http://localhost/profile/customers"
                                            }
                                        }
                                    },
                                    {
                                        name: "orders",
                                        description: "Orders of the invoice",
                                        many_source_per_target: false,
                                        many_target_per_source: true,
                                        _links: {
                                            "blueprint:target-entity": {
                                                href: "http://localhost/profile/orders"
                                            }
                                        }
                                    },
                                    {
                                        name: "refund",
                                        many_source_per_target: false,
                                        many_target_per_source: false,
                                        _links: {
                                            "blueprint:target-entity": {
                                                href: "http://localhost/profile/refunds"
                                            }
                                        }
                                    }
                                ]
                            }
                        }
                        """));
    }

    @Test
    void profileController_missingEntityInformation() throws Exception {
        // If entity is missing in blueprint/datamodel.json, no entity information is shown
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/refunds").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._embedded").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates").exists());
    }
}
