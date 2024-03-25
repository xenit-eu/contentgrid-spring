package com.contentgrid.spring.data.rest.validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.RefundRepository;
import com.contentgrid.spring.test.security.WithMockJwt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = InvoicingApplication.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@WithMockJwt
class ContentGridSpringDataRestValidationConfigurationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    RefundRepository refundRepository;

    @Nested
    class PropertyValidation {
        @Test
        void allowsValidCustomerCreate() throws Exception {
            mockMvc.perform(post("/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "vat": "BE123"
                        }
                        """)
            ).andExpect(status().isCreated());
        }

        @Test
        void allowsValidCustomerUpdate() throws Exception {
            var customer = new Customer();
            customer.setVat("ABC-123");

            customer = customerRepository.save(customer);

            mockMvc.perform(put("/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "vat": "BE456"
                        }
                        """)
            ).andExpect(status().isNoContent());
        }

        @Test
        void allowsValidCustomerPatch() throws Exception {
            var customer = new Customer();
            customer.setVat("ABC-124");

            customer = customerRepository.save(customer);

            mockMvc.perform(patch("/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
            ).andExpect(status().isNoContent());
        }

        @Test
        void rejectsInvalidCustomerCreate() throws Exception {
            mockMvc.perform(post("/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
            ).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsInvalidCustomerUpdate() throws Exception {
            var customer = new Customer();
            customer.setVat("ABC-125");

            customer = customerRepository.save(customer);

            mockMvc.perform(put("/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
            ).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsInvalidCustomerPatch() throws Exception {
            var customer = new Customer();
            customer.setVat("ABC-126");

            customer = customerRepository.save(customer);

            mockMvc.perform(put("/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "vat": null
                        }
                        """)
            ).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RequiredRelation {


        @Test
        void allowsValidInvoiceCreate_withRelation() throws Exception {
            var customer = new Customer();
            customer.setVat("XYZ-1");
            customer = customerRepository.save(customer);

            mockMvc.perform(post("/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "number": "123",
                                "counterparty": "/customers/%s"
                            }
                            """.formatted(customer.getId()))
            ).andExpect(status().isCreated());
        }

        @Test
        void rejectsInvalidInvoiceCreate_missingRelation() throws Exception {
            mockMvc.perform(post("/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "number": "123"
                            }
                            """)
            ).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsInvalidInvoiceCreate_invalidRelation() throws Exception {
            mockMvc.perform(post("/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "number": "123",
                                "counterparty": "XXXX"
                            }
                            """)
            ).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsInvalidInvoiceCreate_nonExistingRelation() throws Exception {
            mockMvc.perform(post("/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "number": "123",
                                "counterparty": "/customers/01bb4210-523b-11ee-9553-e76392218fe8"
                            }
                            """)
            ).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsInvalidInvoiceRelationDelete_requiredRelation() throws Exception {
            var customer = new Customer();
            customer.setVat("XYZ-4");
            customer = customerRepository.save(customer);

            var invoice = new Invoice();
            invoice.setNumber("XYZ-4");
            invoice.setCounterparty(customer);
            invoice = invoiceRepository.save(invoice);

            mockMvc.perform(delete("/invoices/{id}/counterparty", invoice.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Disabled("spring-data-rest throws an IllegalArgumentException here, which we can't handle")
        void rejectsInvalidInvoiceRelationPut_requiredRelation() throws Exception {
            var customer = new Customer();
            customer.setVat("XYZ-5");
            customer = customerRepository.save(customer);

            var invoice = new Invoice();
            invoice.setNumber("XYZ-5");
            invoice.setCounterparty(customer);
            invoice = invoiceRepository.save(invoice);

            mockMvc.perform(put("/invoices/{id}/counterparty", invoice.getId())
                            .contentType(RestMediaTypes.TEXT_URI_LIST)
                            .content("")
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        void allowsValidInvoiceRelationPut_requiredRelation() throws Exception {
            var customer = new Customer();
            customer.setVat("XYZ-6");
            customer = customerRepository.save(customer);

            var invoice = new Invoice();
            invoice.setNumber("XYZ-6");
            invoice.setCounterparty(customer);
            invoice = invoiceRepository.save(invoice);

            var customer2 = new Customer();
            customer2.setVat("XYZ-7");
            customer2 = customerRepository.save(customer2);

            mockMvc.perform(put("/invoices/{id}/counterparty", invoice.getId())
                            .contentType(RestMediaTypes.TEXT_URI_LIST)
                            .content("/customers/" + customer2.getId())
                    )
                    .andExpect(status().is2xxSuccessful());
        }
    }
}