package com.contentgrid.spring.automations.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.contentgrid.spring.automations.rest.AutomationsModel.AutomationModel;
import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.automations.rest.AutomationsModel.AutomationAnnotationModel;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.security.WithMockJwt;
import com.contentgrid.thunx.encoding.json.JsonThunkExpressionCoder;
import com.contentgrid.thunx.predicates.model.Comparison;
import com.contentgrid.thunx.predicates.model.Scalar;
import com.contentgrid.thunx.predicates.model.SymbolicReference;
import com.contentgrid.thunx.predicates.model.ThunkExpression;
import java.util.Base64;
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

@SpringBootTest(properties = "contentgrid.thunx.abac.source=header")
@ContextConfiguration(classes = {
        InvoicingApplication.class,
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@WithMockJwt
class AutomationsRestControllerTest {

    private static final String AUTOMATION_1_ID = UUID.randomUUID().toString();
    private static final String SYSTEM_1_ID = "my-system";
    private static final Map<String, Object> AUTOMATION_DATA = Map.of("foo", "bar");
    private static final String AUTOMATION_2_ID = UUID.randomUUID().toString();
    private static final String SYSTEM_2_ID = "other-system";
    private static final String ENTITY_ANNOTATION_ID = UUID.randomUUID().toString();
    private static final Map<String, String> ENTITY_ANNOTATION_SUBJECT = Map.of("type", "entity", "entity", "customer");
    private static final Map<String, Object> ENTITY_ANNOTATION_DATA = Map.of("color", "blue");
    private static final String ATTRIBUTE_ANNOTATION_ID = UUID.randomUUID().toString();
    private static final Map<String, String> ATTRIBUTE_ANNOTATION_SUBJECT = Map.of("type", "attribute", "entity", "customer", "attribute", "content");
    private static final Map<String, Object> ATTRIBUTE_ANNOTATION_DATA = Map.of("type", "input");
    private static final String RELATION_ANNOTATION_ID = UUID.randomUUID().toString();
    private static final Map<String, String> RELATION_ANNOTATION_SUBJECT = Map.of("type", "relation", "entity", "customer", "relation", "orders");
    private static final Map<String, Object> RELATION_ANNOTATION_DATA = Map.of("type", "output");
    private static final Class<?> ENTITY_CLASS = Customer.class;

    // true = true
    private static final Comparison DEFAULT_POLICY = Comparison.areEqual(Scalar.of(true), Scalar.of(true));

    // automation.system = my-system
    private static final Comparison MY_SYSTEM_POLICY = Comparison.areEqual(
            SymbolicReference.of("entity", path -> path.string("system")),
            Scalar.of("my-system"));

    private static String headerEncode(ThunkExpression<Boolean> expression) {
        var bytes = new JsonThunkExpressionCoder().encode(expression);
        return Base64.getEncoder().encodeToString(bytes);
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
                                .id(AUTOMATION_1_ID)
                                .system(SYSTEM_1_ID)
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
                                .build(),
                        AutomationModel.builder()
                                .id(AUTOMATION_2_ID)
                                .system(SYSTEM_2_ID)
                                .name("other-automation")
                                .data(Map.of())
                                .annotations(List.of(
                                        AutomationAnnotationModel.builder()
                                                .id(RELATION_ANNOTATION_ID)
                                                .subject(RELATION_ANNOTATION_SUBJECT)
                                                .entityClass(ENTITY_CLASS)
                                                .data(RELATION_ANNOTATION_DATA)
                                                .build()
                                ))
                                .build()
                ))
                .build());
    }

    @Test
    void getAutomations_http200() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations")
                        .header("X-ABAC-Context", headerEncode(DEFAULT_POLICY)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            _embedded: {
                                item: [ {
                                    id: "${AUTOMATION_1_ID}",
                                    system: "${SYSTEM_1_ID}",
                                    name: "my-automation",
                                    _links: {
                                        self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_1_ID}" }
                                    }
                                }, {
                                    id: "${AUTOMATION_2_ID}",
                                    system: "${SYSTEM_2_ID}",
                                    name: "other-automation",
                                    _links: {
                                        self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_2_ID}" }
                                    }
                                } ]
                            },
                            _links: {
                                self: { href: "http://localhost/.contentgrid/automations" }
                            }
                        }
                        """.replace("${AUTOMATION_1_ID}", AUTOMATION_1_ID)
                        .replace("${SYSTEM_1_ID}", SYSTEM_1_ID)
                        .replace("${AUTOMATION_2_ID}", AUTOMATION_2_ID)
                        .replace("${SYSTEM_2_ID}", SYSTEM_2_ID)))
                .andExpect(jsonPath("$._embedded.item[0].data").doesNotExist());
    }

    @Test
    void getAutomations_withPolicy_http200() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations")
                        .header("X-ABAC-Context", headerEncode(MY_SYSTEM_POLICY)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                            {
                                _embedded: {
                                    item: [ {
                                        id: "${AUTOMATION_1_ID}",
                                        system: "${SYSTEM_1_ID}",
                                        name: "my-automation",
                                        _links: {
                                            self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_1_ID}" }
                                        }
                                    } ]
                                },
                                _links: {
                                    self: { href: "http://localhost/.contentgrid/automations" }
                                }
                            }
                            """.replace("${AUTOMATION_1_ID}", AUTOMATION_1_ID)
                        .replace("${SYSTEM_1_ID}", SYSTEM_1_ID)))
                .andExpect(jsonPath("$._embedded.item[0].data").doesNotExist())
                .andExpect(jsonPath("$._embedded.item", hasSize(1)));
    }

    @Test
    void getAutomation_http200() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations/{id}", AUTOMATION_1_ID)
                        .header("X-ABAC-Context", headerEncode(DEFAULT_POLICY)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            id: "${AUTOMATION_1_ID}",
                            system: "${SYSTEM_1_ID}",
                            name: "my-automation",
                            data: {
                                foo: "bar"
                            },
                            _embedded: {
                                "automation:annotation": [ {
                                    id: "${ENTITY_ANNOTATION_ID}",
                                    subject: {
                                        type: "entity",
                                        entity: "customer"
                                    },
                                    data: {
                                        color: "blue"
                                    },
                                    _links: {
                                        "automation:target-entity": {
                                            href: "http://localhost/profile/customers"
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
                                        "automation:target-entity": {
                                            href: "http://localhost/profile/customers"
                                        }
                                    }
                                } ]
                            },
                            _links: {
                                self: { href: "http://localhost/.contentgrid/automations/${AUTOMATION_1_ID}" }
                            }
                        }
                        """.replace("${AUTOMATION_1_ID}", AUTOMATION_1_ID)
                        .replace("${SYSTEM_1_ID}", SYSTEM_1_ID)
                        .replace("${ENTITY_ANNOTATION_ID}", ENTITY_ANNOTATION_ID)
                        .replace("${ATTRIBUTE_ANNOTATION_ID}", ATTRIBUTE_ANNOTATION_ID)));
    }

    @Test
    void getAutomation_wrongId_http404() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations/{id}", UUID.randomUUID().toString())
                        .header("X-ABAC-Context", headerEncode(DEFAULT_POLICY)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAutomation_noAccess_http404() throws Exception {
        mockMvc.perform(get("/.contentgrid/automations/{id}", AUTOMATION_2_ID)
                        .header("X-ABAC-Context", headerEncode(MY_SYSTEM_POLICY)))
                .andExpect(status().isNotFound());
    }

}