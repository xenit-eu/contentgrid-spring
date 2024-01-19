package com.contentgrid.spring.data.rest.messages;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import java.util.Set;
import java.util.UUID;
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
class HalLinkTitlesAndFormPromptsTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    InvoiceRepository invoiceRepository;

    Customer customer;
    Invoice invoice;

    @BeforeEach
    void setup() {
        customer = customerRepository.save(new Customer(UUID.randomUUID(), "Abc", "ABC", null, null, null, Set.of(), Set.of()));
        invoice = invoiceRepository.save(new Invoice("12345678", true, true, customer, Set.of()));
    }

    @AfterEach
    void cleanup() {
        invoiceRepository.delete(invoice);
        // We delete by id because the object is no longer valid after the invoice it references has been deleted
        customerRepository.deleteById(customer.getId());
        invoice = null;
        customer = null;
    }

    @Test
    void promptOnCreateFormPropertiesInHalForms() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                search: {
                                    method: "GET",
                                    properties: [
                                        {
                                            prompt: "VAT number",
                                            name: "vat",
                                            type: "text"
                                        },
                                        { name: "content.size", type: "number" }, { name: "content.mimetype", type: "text" }, { name: "content.filename", type: "text" },
                                        { name: "invoices.number", type: "text" }, { name: "invoices.paid", type: "checkbox" }, { name: "invoices.orders.id" }, { name: "invoices.content.length", type: "number" }
                                    ]
                                },
                                create-form: {
                                    method: "POST",
                                    properties: [
                                        {
                                            prompt: "Customer name",
                                            name: "name",
                                            type: "text"
                                        },
                                        {
                                            prompt: "VAT number",
                                            name: "vat",
                                            type: "text"
                                        },
                                        {
                                            name : "birthday",
                                            type : "datetime"
                                        },
                                        {
                                            prompt: "Spending Total",
                                            name : "total_spend",
                                            type : "number"
                                        },
                                        {
                                            prompt: "Customer Document Mimetype",
                                            name: "content.mimetype",
                                            type: "text"
                                        },
                                        {
                                            prompt: "Customer Document Filename",
                                            name: "content.filename",
                                            type: "text"
                                        },
                                        { name : "orders", type : "url" }, { name : "invoices", type : "url" }
                                    ]
                                }
                            }
                        }
                        """))
        ;
    }

    @Test
    void titleOnCgEntityInHalForms() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                "cg:entity": [
                                    {
                                        name: "customers",
                                        title: "Client"
                                    },
                                    { name: "invoices" }, { name: "refunds" }, { name: "promotions" },
                                    { name: "shipping-addresses" }, { name: "orders" }
                                ]
                            }
                        }
                        """))
        ;
    }

    @Test
    void titleOnCgRelationInHalForms() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/invoices/" + invoice.getId()).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                "cg:relation": [
                                    {
                                        name: "counterparty",
                                        title: "Sent by"
                                    },
                                    { name: "orders" }, { name: "refund" }
                                ]
                            }
                        }
                        """))
                ;
    }

    @Test
    void titleOnCgContentInHalForms() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/invoices/" + invoice.getId()).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                "cg:content": [
                                    {
                                        name: "attachment",
                                        title: "Attached File"
                                    },
                                    { name: "content" }
                                ]
                            }
                        }
                        """))
        ;
    }

    @Test
    void promptOnCgContentPropertiesInHalForms() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/invoices").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(res -> System.out.println(res.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                "create-form": {
                                    properties: [
                                        {
                                            prompt: "Attached File Filename",
                                            name: "attachment_filename",
                                            type: "text"
                                        },
                                        {
                                            prompt: "Attached File Mimetype",
                                            name: "attachment_mimetype",
                                            type: "text"
                                        },
                                        {},{},{},{},{},{},{},{}
                                    ]
                                }
                            }
                        }
                        """))
        ;
    }

    @Test
    void titleOnEntityAndPropertiesInJsonSchema() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                        .accept("application/schema+json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            title : "Client",
                            properties : {
                                name : {
                                    title : "Customer name",
                                    readOnly : false,
                                    type : "string"
                                },
                                vat : {
                                    title : "VAT number",
                                    readOnly : false,
                                    type : "string"
                                },
                                birthday : {
                                    title : "Birthday",
                                    readOnly : false,
                                    type : "string",
                                    format : "date-time"
                                },
                                total_spend : {
                                    title : "Total Amount Spent",
                                    readOnly : false,
                                    type : "integer"
                                }
                            }
                        }
                        """))
        ;
    }

}