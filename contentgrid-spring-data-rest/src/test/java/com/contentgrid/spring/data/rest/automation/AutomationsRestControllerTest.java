package com.contentgrid.spring.data.rest.automation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AutomationAnnotationModel;
import com.contentgrid.spring.data.rest.automation.AutomationsModel.AutomationModel;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.security.WithMockJwt;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@WithMockJwt
class AutomationsRestControllerTest {

    private static final String AUTOMATION_ID = UUID.randomUUID().toString();
    private static final String SYSTEM_ID = "my-system";
    private static final Map<String, Object> AUTOMATION_DATA = Map.of("foo", "bar");
    private static final String ENTITY_ANNOTATION_ID = UUID.randomUUID().toString();
    private static final Map<String, String> ENTITY_ANNOTATION_SUBJECT = Map.of("type", "entity", "entity", "customer");
    private static final Map<String, Object> ENTITY_ANNOTATION_DATA = Map.of("color", "blue");
    private static final String ATTRIBUTE_ANNOTATION_ID = UUID.randomUUID().toString();
    private static final Map<String, String> ATTRIBUTE_ANNOTATION_SUBJECT = Map.of("type", "attribute", "entity", "customer", "attribute", "content");
    private static final Map<String, Object> ATTRIBUTE_ANNOTATION_DATA = Map.of("type", "input");
    private static final Class<?> ENTITY_CLASS;

    static {
        try {
            ENTITY_CLASS = Class.forName("com.contentgrid.spring.test.fixture.invoicing.model.Customer");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AutomationsRestController controller;

    @BeforeEach
    void setup() {
        controller.setModel(AutomationsModel.builder()
                .automations(List.of(
                        AutomationModel.builder()
                                .id(AUTOMATION_ID)
                                .system(SYSTEM_ID)
                                .name("my-automation")
                                .data(AUTOMATION_DATA)
                                .annotations(List.of(
                                        AutomationAnnotationModel.builder()
                                                .id(ENTITY_ANNOTATION_ID)
                                                .subject(ENTITY_ANNOTATION_SUBJECT)
                                                .entityClass(ENTITY_CLASS)
                                                .data(ENTITY_ANNOTATION_DATA)
                                                .build(),
                                        AutomationAnnotationModel.builder()
                                                .id(ATTRIBUTE_ANNOTATION_ID)
                                                .subject(ATTRIBUTE_ANNOTATION_SUBJECT)
                                                .entityClass(ENTITY_CLASS)
                                                .data(ATTRIBUTE_ANNOTATION_DATA)
                                                .build()
                                ))
                                .build()
                ))
                .build());
    }

    @Test
    void getAutomations_http200() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            _embedded: {
                                item: [ {
                                    id: "${AUTOMATION_ID}",
                                    system: "${SYSTEM_ID}",
                                    name: "my-automation",
                                    _links: {
                                        self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_ID}" }
                                    }
                                } ]
                            },
                            _links: {
                                self: { href: "http://localhost/.contentgrid/automations" }
                            }
                        }
                        """.replace("${AUTOMATION_ID}", AUTOMATION_ID)
                        .replace("${SYSTEM_ID}", SYSTEM_ID)))
                .andExpect(jsonPath("$._embedded.item[0].data").doesNotExist());
    }

    @Test
    void getAutomation_http200() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations/{id}", AUTOMATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            id: "${AUTOMATION_ID}",
                            system: "${SYSTEM_ID}",
                            name: "my-automation",
                            data: {
                                foo: "bar"
                            },
                            _embedded: {
                                "cg:automation-annotation": [ {
                                    id: "${ENTITY_ANNOTATION_ID}",
                                    subject: {
                                        type: "entity",
                                        entity: "customer"
                                    },
                                    data: {
                                        color: "blue"
                                    },
                                    _links: {
                                        "cg:entity-profile": {
                                            href: "http://localhost/profile/customers"
                                        },
                                        "cg:entity": {
                                            href: "http://localhost/customers{?page,size,sort*}",
                                            templated: true
                                        }
                                    }
                                },
                                {
                                    id: "${ATTRIBUTE_ANNOTATION_ID}",
                                    subject: {
                                        type: "attribute",
                                        entity: "customer",
                                        attribute: "content"
                                    },
                                    data: {
                                        type: "input"
                                    },
                                    _links: {
                                        "cg:entity-profile": {
                                            href: "http://localhost/profile/customers"
                                        },
                                        "cg:entity": {
                                            href: "http://localhost/customers{?page,size,sort*}",
                                            templated: true
                                        }
                                    }
                                } ]
                            },
                            _links: {
                                self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_ID}" }
                            }
                        }
                        """.replace("${AUTOMATION_ID}", AUTOMATION_ID)
                        .replace("${SYSTEM_ID}", SYSTEM_ID)
                        .replace("${ENTITY_ANNOTATION_ID}", ENTITY_ANNOTATION_ID)
                        .replace("${ATTRIBUTE_ANNOTATION_ID}", ATTRIBUTE_ANNOTATION_ID)));
    }

    @Test
    void getAutomation_wrongId_http404() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }
}