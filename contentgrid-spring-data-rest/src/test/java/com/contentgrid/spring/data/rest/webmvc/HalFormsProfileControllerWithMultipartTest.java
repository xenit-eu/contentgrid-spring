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

@SpringBootTest(properties = { "contentgrid.rest.use-multipart-hal-forms=true" })
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
class HalFormsProfileControllerWithMultipartTest extends AbstractHalFormsProfileControllerTest {

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
                                    contentType: "multipart/form-data",
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
                                            name: "content",
                                            type: "file"
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
                                search: {} # tested in parent class
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
                                    contentType: "multipart/form-data",
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
                                            name: "content",
                                            type: "file"
                                        },
                                        {
                                           name: "attachment",
                                           type: "file"
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
                                search: {} # tested in parent class
                            }
                        }
                        """));
    }
}