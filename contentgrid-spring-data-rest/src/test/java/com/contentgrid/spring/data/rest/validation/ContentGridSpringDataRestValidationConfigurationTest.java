package com.contentgrid.spring.data.rest.validation;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = InvoicingApplication.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class ContentGridSpringDataRestValidationConfigurationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void allowsValidCustomerCreate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "vat": "BE123"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void allowsValidCustomerUpdate() throws Exception {
        var customer = new Customer();
        customer.setVat("ABC-123");

        customer = customerRepository.save(customer);

        mockMvc.perform(MockMvcRequestBuilders.put("/customers/{id}", customer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "vat": "BE456"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void allowsValidCustomerPatch() throws Exception {
        var customer = new Customer();
        customer.setVat("ABC-124");

        customer = customerRepository.save(customer);

        mockMvc.perform(MockMvcRequestBuilders.patch("/customers/{id}", customer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void rejectsInvalidCustomerCreate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void rejectsInvalidCustomerUpdate() throws Exception {
        var customer = new Customer();
        customer.setVat("ABC-125");

        customer = customerRepository.save(customer);

        mockMvc.perform(MockMvcRequestBuilders.put("/customers/{id}", customer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "XYZ"
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void rejectsInvalidCustomerPatch() throws Exception {
        var customer = new Customer();
        customer.setVat("ABC-126");

        customer = customerRepository.save(customer);

        mockMvc.perform(MockMvcRequestBuilders.put("/customers/{id}", customer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "vat": null
                        }
                        """)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

}