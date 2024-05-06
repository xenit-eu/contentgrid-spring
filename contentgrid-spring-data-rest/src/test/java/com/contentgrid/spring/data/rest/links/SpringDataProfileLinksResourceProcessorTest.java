package com.contentgrid.spring.data.rest.links;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.security.WithMockJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class
})
@AutoConfigureMockMvc
@WithMockJwt
class SpringDataProfileLinksResourceProcessorTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void entityLinkRelAddedToProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                "cg:entity": [
                                    {
                                        name: "customers",
                                        href: "http://localhost/profile/customers"
                                    },
                                    {
                                        name: "invoices",
                                        href: "http://localhost/profile/invoices"
                                    },
                                    {
                                        name: "orders",
                                        href: "http://localhost/profile/orders"
                                    },
                                    {
                                        name: "promotions",
                                        href: "http://localhost/profile/promotions"
                                    },
                                    {
                                        name: "shipping-addresses",
                                        href: "http://localhost/profile/shipping-addresses"
                                    },
                                    {
                                        name: "labels",
                                        href: "http://localhost/profile/labels"
                                    },
                                    {
                                        name: "refunds",
                                        href: "http://localhost/profile/refunds"
                                    }
                                ]
                            }
                        }
                        """));
    }
}