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
class SpringDataRepositoryLinksResourceProcessorTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void entityLinkRelAddedToRoot() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _links: {
                                "cg:entity": [
                                    {
                                        name: "customers",
                                        href: "http://localhost/customers{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "invoices",
                                        href: "http://localhost/invoices{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "orders",
                                        href: "http://localhost/orders{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "promotions",
                                        href: "http://localhost/promotions{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "shipping-addresses",
                                        href: "http://localhost/shipping-addresses{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "shipping-labels",
                                        href: "http://localhost/shipping-labels{?page,size,sort}",
                                        templated: true
                                    },
                                    {
                                        name: "refunds",
                                        href: "http://localhost/refunds{?page,size,sort}",
                                        templated: true
                                    }
                                ]
                            }
                        }
                        """));
    }

}