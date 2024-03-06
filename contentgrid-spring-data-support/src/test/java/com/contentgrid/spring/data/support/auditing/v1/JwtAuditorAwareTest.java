package com.contentgrid.spring.data.support.auditing.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootTest
@ContextConfiguration
class JwtAuditorAwareTest {

    private MockMvc mockMvc;

    @Autowired
    InvoiceRepository invoices;

    @Autowired
    WebApplicationContext context;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .alwaysDo(print())
                .build();
    }

    @AfterEach
    public void cleanup() {
        invoices.deleteAll();
    }

    static JwtRequestPostProcessor jwtWithClaims(String subject, String name) {
        return jwt().jwt(jwt -> jwt.subject(subject).claim("name", name)
                .issuer("http://localhost/realms/cg-invalid"));
    }

    static JwtRequestPostProcessor john = jwtWithClaims("123456", "john smith");
    static JwtRequestPostProcessor bob = jwtWithClaims("abcdef", "bob admin");

    @Test
    void postEntity_shouldSetAuditMetadataFields_http201() throws Exception {
        mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"number": "123456"}
                                """)
                        .with(john))
                .andExpect(status().isCreated());
        var dateAfterCreation = Instant.now();

        mockMvc.perform(get("/invoices").with(john))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].number").value("123456"))
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].auditing").exists())
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].auditing.created_by").value("john smith"))
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].auditing.created_date").exists())
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].auditing.last_modified_by").value("john smith"))
                .andExpect(jsonPath("$._embedded.['d:invoices'][0].auditing.last_modified_date").exists());

        assertThat(invoices.findAll()).singleElement().satisfies(invoice -> {
            assertThat(invoice.getNumber()).isEqualTo("123456");
            assertThat(invoice.getAuditing().getCreatedBy().getId()).isEqualTo("123456");
            assertThat(invoice.getAuditing().getCreatedDate()).isBefore(dateAfterCreation);
            assertThat(invoice.getAuditing().getLastModifiedBy().getNamespace()).isEqualTo("http://localhost/realms/cg-invalid");
            assertThat(invoice.getAuditing().getLastModifiedDate()).isBefore(dateAfterCreation);
        });
    }

    @Test
    void putEntity_shouldUpdateAuditMetadataFields_http204() throws Exception {
        var response = mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"number": "123456"}
                                """)
                        .with(john))
                .andExpect(status().isCreated())
                .andReturn();
        var invoiceId = StringUtils.substringAfterLast(response.getResponse().getHeader("Location"), "/");
        var dateAfterCreation = Instant.now();

        mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"number": "000000"}
                                """)
                        .with(bob))
                .andExpect(status().isNoContent());
        assertThat(invoices.findAll()).singleElement().satisfies(invoice -> {
            assertThat(invoice.getNumber()).isEqualTo("000000");
            assertThat(invoice.getAuditing().getCreatedBy().getName()).isEqualTo("john smith");
            assertThat(invoice.getAuditing().getCreatedDate()).isBefore(dateAfterCreation);
            assertThat(invoice.getAuditing().getLastModifiedBy().getName()).isEqualTo("bob admin");
            assertThat(invoice.getAuditing().getLastModifiedDate()).isAfter(dateAfterCreation);
        });
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true)
    static class TestApp {

    }

    @Entity
    @NoArgsConstructor
    @Getter
    @Setter
    @EntityListeners(AuditingEntityListener.class)
    static class Invoice {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private UUID id;

        private String number;

        @Embedded
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @AttributeOverride(name = "createdBy.id", column = @Column(name = "auditing__created_by__id"))
        @AttributeOverride(name = "createdBy.namespace", column = @Column(name = "auditing__created_by__namespace"))
        @AttributeOverride(name = "createdBy.name", column = @Column(name = "auditing__created_by__name"))
        @AttributeOverride(name = "createdDate", column = @Column(name = "auditing__created_date"))
        @AttributeOverride(name = "lastModifiedBy.id", column = @Column(name = "auditing__last_modified_by__id"))
        @AttributeOverride(name = "lastModifiedBy.namespace", column = @Column(name = "auditing__last_modified_by__namespace"))
        @AttributeOverride(name = "lastModifiedBy.name", column = @Column(name = "auditing__last_modified_by__name"))
        @AttributeOverride(name = "lastModifiedDate", column = @Column(name = "auditing__last_modified_date"))
        private AuditMetadata auditing = new AuditMetadata();
    }

    @RepositoryRestResource(collectionResourceRel = "d:invoices", itemResourceRel = "d:invoice")
    interface InvoiceRepository extends JpaRepository<Invoice, UUID>, QuerydslPredicateExecutor<Invoice> {

    }

    @Configuration
    @EnableJpaAuditing
    static class RestConfiguration implements RepositoryRestConfigurer {

        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config,
                CorsRegistry cors) {
            config.exposeIdsFor(Invoice.class);
        }

        @Bean
        public AuditorAware<UserMetadata> auditorProvider() {
            return new JwtAuditorAware();
        }
    }
}