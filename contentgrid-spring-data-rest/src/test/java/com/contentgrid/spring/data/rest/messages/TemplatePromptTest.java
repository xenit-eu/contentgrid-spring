package com.contentgrid.spring.data.rest.messages;

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
class TemplatePromptTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    Customer customer;

    @BeforeEach
    void setup() {
        customer = customerRepository.save(new Customer(UUID.randomUUID(), "Abc", "ABC", null, null, null, Set.of(), Set.of()));
    }

    @AfterEach
    void cleanup() {
        customerRepository.delete(customer);
        customer = null;
    }

    @Test
    void promptOnVatAndNameInHalForms() throws Exception {
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
                                            name : "total_spend",
                                            type : "number"
                                        },
                                        { name: "content.mimetype", type: "text" }, { name: "content.filename", type: "text" },
                                        { name : "orders", type : "url" }, { name : "invoices", type : "url" }
                                    ]
                                }
                            }
                        }
                        """))
                ;
    }

    @Test
    void titleOnVatAndNameInJsonSchema() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/customers")
                        .accept("application/schema+json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            title : "Customer",
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
                                    title : "Total spend",
                                    readOnly : false,
                                    type : "integer"
                                }
                            }
                        }
                        """))
        ;
    }

}