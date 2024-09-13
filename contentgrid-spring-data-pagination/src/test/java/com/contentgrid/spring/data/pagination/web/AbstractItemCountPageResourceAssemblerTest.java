package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.data.pagination.ItemCount;
import com.contentgrid.spring.data.pagination.jpa.strategy.JpaQuerydslItemCountStrategy;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@AutoConfigureMockMvc
abstract class AbstractItemCountPageResourceAssemblerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository customerRepository;

    static ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jackson2HalModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @MockBean
    JpaQuerydslItemCountStrategy countingStrategy;

    @AfterEach
    void cleanup() {
        customerRepository.deleteAll();
    }

    void setupCustomers(int amount) {
        customerRepository.saveAllAndFlush(
                IntStream.range(0, amount)
                        .mapToObj(number -> {
                            var customer = new Customer();
                            customer.setName("Customer %d".formatted(number));
                            customer.setVat("VAT%d".formatted(number));
                            return customer;
                        })
                        .toList()
        );
    }

    abstract ResultMatcher[] createLegacyResultMatchers(int expectedItemCount);

    @Test
    void emptyPage() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_exact").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_estimate").doesNotExist())
                .andExpectAll(createLegacyResultMatchers(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.first.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.next.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.last.href").doesNotExist());
    }

    @Test
    void multiplePages_estimatedCount() throws Exception {
        setupCustomers(25);

        Mockito.when(countingStrategy.countQuery(Mockito.any())).thenReturn(Optional.of(ItemCount.estimated(120)));

        var firstPageResult = mvc.perform(MockMvcRequestBuilders.get("/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_exact").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_estimate").value("~120"))
                .andExpectAll(createLegacyResultMatchers(120))
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.first.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.next.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.last.href").doesNotExist())
                .andReturn();

        var firstPage = objectMapper.readValue(firstPageResult.getResponse().getContentAsByteArray(),
                new TypeReference<CollectionModel<?>>() {
                });

        mvc.perform(MockMvcRequestBuilders.get(firstPage.getRequiredLink(IanaLinkRelations.NEXT).toUri()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_exact").value(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_estimate").doesNotExist())
                .andExpectAll(createLegacyResultMatchers(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.first.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.next.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.prev.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.last.href").doesNotExist());

    }

    @Test
    void multiplePages_exactCount() throws Exception {
        setupCustomers(25);

        Mockito.when(countingStrategy.countQuery(Mockito.any())).thenReturn(Optional.of(ItemCount.exact(25)));

        var firstPageResult = mvc.perform(MockMvcRequestBuilders.get("/customers")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_exact").value(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_estimate").doesNotExist())
                .andExpectAll(createLegacyResultMatchers(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.first.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.next.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.last.href").doesNotExist())
                .andReturn();

        var firstPage = objectMapper.readValue(firstPageResult.getResponse().getContentAsByteArray(),
                new TypeReference<CollectionModel<?>>() {
                });

        mvc.perform(MockMvcRequestBuilders.get(firstPage.getRequiredLink(IanaLinkRelations.NEXT).toUri()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.number").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_exact").value(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.total_items_estimate").doesNotExist())
                .andExpectAll(createLegacyResultMatchers(25))
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.first.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.next.href").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.prev.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$._links.last.href").doesNotExist());

    }
}