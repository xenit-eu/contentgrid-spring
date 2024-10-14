package com.contentgrid.spring.data.rest.messages;

import com.contentgrid.spring.data.rest.messages.HalLinkTitlesAndFormPromptsTest.LocalConfiguration;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingLabel;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.ShippingLabelRepository;
import com.contentgrid.spring.test.security.WithMockJwt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(properties = { "contentgrid.rest.use-multipart-hal-forms=true" })
@ContextConfiguration(classes = {
        InvoicingApplication.class,
        LocalConfiguration.class
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@WithMockJwt
class HalLinkTitlesAndFormPromptsTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    InvoiceRepository invoiceRepository;
    @Autowired
    ShippingLabelRepository shippingLabelRepository;

    Customer customer;
    Invoice invoice;
    ShippingLabel shippingLabel;

    @BeforeEach
    void setup() {
        customer = customerRepository.save(new Customer("Abc", "ABC"));
        invoice = invoiceRepository.save(new Invoice("12345678", true, true, customer, Set.of()));
        shippingLabel = shippingLabelRepository.save(new ShippingLabel("here", "there"));
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
                                        {
                                            name: "birthday",
                                            type: "datetime"
                                        },
                                        {
                                            name: "gender",
                                            type: "text"
                                        },
                                        { name: "content.size", type: "number" }, { name: "content.mimetype", type: "text" }, { name: "content.filename", type: "text" },
                                        { name: "invoices.number", type: "text" }, { name: "invoices.paid", type: "checkbox" }, { name: "invoices.orders.id" }, { name: "invoices.content.length", type: "number" },
                                        {
                                            name: "sort",
                                            options: {
                                                promptField: "prompt",
                                                valueField: "value",
                                                inline: [
                                                    {
                                                        value: "vat,asc",
                                                        prompt: "VAT number A→Z" # Note that the field value is replaced with the proper translation
                                                    },
                                                    {
                                                        value: "vat,desc",
                                                        prompt: "VAT number Z→A"
                                                    },
                                                    {
                                                        value: "birthday,asc",
                                                        prompt: "birthday oldest first"
                                                    },
                                                    {
                                                        value: "birthday,desc",
                                                        prompt: "birthday newest first"
                                                    },
                                                    {
                                                        value: "gender,asc",
                                                        prompt: "gender A→Z"
                                                    },
                                                    {
                                                        value: "gender,desc",
                                                        prompt: "gender Z→A"
                                                    },
                                                    {
                                                        value: "content.size,asc",
                                                        prompt: "content.size 0→9"
                                                    },
                                                    {
                                                        value: "content.size,desc",
                                                        prompt: "content.size 9→0"
                                                    },
                                                    {
                                                        value: "content.mimetype,asc",
                                                        prompt: "Customer Document Mimetype A→Z"
                                                    },
                                                    {
                                                        value: "content.mimetype,desc",
                                                        prompt: "Customer Document Mimetype Z→A"
                                                    },
                                                    {
                                                        value: "content.filename,asc",
                                                        prompt: "Customer Document Filename A→Z"
                                                    },
                                                    {
                                                        value: "content.filename,desc",
                                                        prompt: "Customer Document Filename Z→A"
                                                    }
                                                ]
                                            }
                                        }
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
                                            name: "content",
                                            type: "file"
                                        },
                                        {
                                            name : "birthday",
                                            type : "datetime"
                                        },
                                        {
                                            name: "gender",
                                            type: "radio",
                                            options: {
                                                inline: [ "female", "male" ]
                                            }
                                        },
                                        {
                                            prompt: "Spending Total",
                                            name : "total_spend",
                                            type : "number"
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
    void sortPropertyFallback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/entity-with-sort")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                search: {
                                    method: "GET",
                                    properties: [
                                        {
                                            name: "id"
                                        },
                                        {
                                            name: "sort",
                                            type: "text",
                                            "options" : {
                                                "promptField" : "prompt",
                                                "valueField" : "value",
                                                "minItems" : 0,
                                                "inline" : [
                                                    {
                                                        "property" : "id",
                                                        "direction" : "asc",
                                                        "prompt" : "id ascending",
                                                        "value" : "id,asc"
                                                    }, {
                                                        "property" : "id",
                                                        "direction" : "desc",
                                                        "prompt" : "id descending",
                                                        "value" : "id,desc"
                                                    }
                                                ]
                                            }
                                        }
                                    ],
                                    target: "http://localhost/entity-with-sort"
                                },
                                "create-form": {
                                    method: "POST",
                                    properties: [
                                        {
                                            name: "sort",
                                            type: "text"
                                        }
                                    ],
                                    target: "http://localhost/entity-with-sort"
                                }
                            }
                        }
                        """))
                // Check that options is certainly not present in the create-form field
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.['create-form'].properties[0].options")
                        .doesNotExist());
    }

    @Test
    void noSortPropertyWithoutCollectionFilterParams() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/entity-without-filters")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                search: {
                                    method: "GET",
                                    properties: [],
                                    target: "http://localhost/entity-without-filters"
                                },
                                "create-form": {
                                    method: "POST",
                                    properties: [
                                        {
                                            name: "name",
                                            type: "text"
                                        }
                                    ],
                                    target: "http://localhost/entity-without-filters"
                                }
                            }
                        }
                        """))
                // Check that there are no search or sort parameters present
                .andExpect(MockMvcResultMatchers.jsonPath("$._templates.search.properties")
                        .isEmpty());
    }

    @Test
    void contentFieldCamelCasedInCreateForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/profile/shipping-labels")
                        .accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                search: {},
                                create-form: {
                                    method: "POST",
                                    properties: [
                                        {
                                            name: "from",
                                            type: "text",
                                            required: true
                                        },
                                        {
                                            name: "to",
                                            type: "text",
                                            required: true
                                        },
                                        {
                                            name: "parent",
                                            type: "url"
                                        },
                                        {
                                            name: "barcodePicture",
                                            type: "file"
                                        },
                                        {
                                            name: "_package",
                                            type: "file"
                                        }
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
                                    { name: "shipping-addresses" }, { name: "shipping-labels" }, { name: "orders" },
                                    { name: "entity-with-sort" }, { name: "entity-without-filters" }
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
        mockMvc.perform(MockMvcRequestBuilders.get("/invoices/" + invoice.getId()).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(res -> System.out.println(res.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.content().json("""
                        {
                            _templates: {
                                "default": {
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
                                        {},{},{},{},{}
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


    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {HalLinkTitlesAndFormPromptsTest.class, InvoicingApplication.class})
    @EnableJpaRepositories(basePackageClasses = {InvoicingApplication.class,
            HalLinkTitlesAndFormPromptsTest.class}, considerNestedRepositories = true)
    static class LocalConfiguration {


    }

    @Entity
    public static class EntityWithSort {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @JsonProperty(access = Access.READ_ONLY)
        @CollectionFilterParam
        private UUID id;

        private String sort;
    }

    @RepositoryRestResource(path = "entity-with-sort", collectionResourceRel = "d:entity-with-sort", itemResourceRel = "d:entity-with-sort")
    public interface EntityWithSortRepository extends
            JpaRepository<EntityWithSort, UUID>,
            QuerydslPredicateExecutor<EntityWithSort> {

    }

    @Entity
    public static class EntityWithoutFilters {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @JsonProperty(access = Access.READ_ONLY)
        private UUID id;

        private String name;
    }

    @RepositoryRestResource(path = "entity-without-filters", collectionResourceRel = "d:entity-without-filters", itemResourceRel = "d:entity-without-filters")
    public interface EntityWithoutCollectionFilterParamRepository extends
            JpaRepository<EntityWithoutFilters, UUID>,
            QuerydslPredicateExecutor<EntityWithoutFilters> {

    }
}
