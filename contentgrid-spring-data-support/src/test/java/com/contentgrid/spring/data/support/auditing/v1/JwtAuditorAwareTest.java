package com.contentgrid.spring.data.support.auditing.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootTest(properties = {"spring.content.storage.type.default = fs"})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class JwtAuditorAwareTest {

    private static final String JWT_ISSUER_NAMESPACE = "http://localhost/realms/cg-invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    InvoiceRepository invoices;

    @MockBean(name = "mockedDateTimeProvider")
    DateTimeProvider mockedDateTimeProvider;

    @AfterEach
    public void cleanup() {
        invoices.deleteAll();
    }

    static JwtRequestPostProcessor jwtWithClaims(String subject, String name) {
        return jwt().jwt(jwt -> jwt
                .subject(subject)
                .claim("name", name)
                .issuer(JWT_ISSUER_NAMESPACE)
        );
    }

    static JwtRequestPostProcessor jwtWithSubject(String subject) {
        return jwt().jwt(jwt -> jwt.subject(subject).issuer(JWT_ISSUER_NAMESPACE));
    }

    @Test
    void postEntity_shouldSetAuditMetadataFields_http201() throws Exception {
        var timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(timestamp));

        var response = mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "number": "123456"
                                }
                                """)
                        .with(jwtWithClaims("user-id-5", "john smith")))
                .andExpect(status().isCreated())
                .andReturn();

        var location = Objects.requireNonNull(response.getResponse().getHeader(HttpHeaders.LOCATION));
        var invoiceId = StringUtils.substringAfterLast(location, "/");

        assertThat(invoices.findById(UUID.fromString(invoiceId)))
                .isPresent()
                .hasValueSatisfying(invoice -> assertThat(invoice.getAuditing()).satisfies(auditing -> {
                    assertThat(auditing.getCreatedBy().getId()).isEqualTo("user-id-5");
                    assertThat(auditing.getCreatedBy().getName()).isEqualTo("john smith");
                    assertThat(auditing.getCreatedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getCreatedBy()).isEqualTo(auditing.getLastModifiedBy());

                    assertThat(auditing.getCreatedDate())
                            .isEqualTo(auditing.getLastModifiedDate())
                            .isEqualTo(timestamp);
                }));
    }

    @Test
    void postEntity_missingNameClaimDefaultsToSubject_http201() throws Exception {
        var subject = "user-id-3";
        var response = mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "number": "123456"
                                }
                                """)
                        .with(jwtWithSubject(subject)))
                .andExpect(status().isCreated())
                .andReturn();

        var location = Objects.requireNonNull(response.getResponse().getHeader("Location"));
        var invoiceId = StringUtils.substringAfterLast(location, "/");

        assertThat(invoices.findById(UUID.fromString(invoiceId)))
                .isPresent()
                .hasValueSatisfying(invoice -> assertThat(invoice.getAuditing()).satisfies(auditing -> {
                    assertThat(auditing.getCreatedBy().getId()).isEqualTo(subject);
                    assertThat(auditing.getCreatedBy().getName()).isEqualTo(subject);
                    assertThat(auditing.getCreatedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getCreatedBy()).isEqualTo(auditing.getLastModifiedBy());
                }));
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void putEntity_shouldUpdateAuditMetadataFields_http204() throws Exception {
        var created = Instant.now().minusSeconds(1000).truncatedTo(ChronoUnit.MILLIS);
        var modified = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(created), Optional.of(modified));

        var response = mockMvc.perform(post("/invoices")
                        .with(jwtWithClaims("user-id-2", "john"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "number": "123456"
                                }
                                """)
                       )
                .andExpect(status().isCreated())
                .andReturn();
        var location = Objects.requireNonNull(response.getResponse().getHeader("Location"));
        var invoiceId = StringUtils.substringAfterLast(location, "/");

        mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtWithClaims("user-id-3", "bob"))
                        .content("""
                                {
                                    "number": "000000"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(invoices.findById(UUID.fromString(invoiceId)))
                .isPresent()
                .hasValueSatisfying(invoice -> assertThat(invoice.getAuditing()).satisfies(auditing -> {
                    assertThat(auditing.getCreatedBy().getId()).isEqualTo("user-id-2");
                    assertThat(auditing.getCreatedBy().getName()).isEqualTo("john");
                    assertThat(auditing.getCreatedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getCreatedDate()).isEqualTo(created);

                    assertThat(auditing.getLastModifiedBy().getId()).isEqualTo("user-id-3");
                    assertThat(auditing.getLastModifiedBy().getName()).isEqualTo("bob");
                    assertThat(auditing.getLastModifiedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getLastModifiedDate()).isEqualTo(modified);
                }));
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void updatingAuditableMetadata_shouldBeIgnored() throws Exception {
        var created = Instant.now().minusSeconds(1000).truncatedTo(ChronoUnit.MILLIS);
        var modified = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Mockito.when(mockedDateTimeProvider.getNow()).thenReturn(Optional.of(created), Optional.of(modified));

        var response = mockMvc.perform(post("/invoices")
                        .with(jwtWithClaims("user-id-2", "john"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "number": "123456"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn();
        var location = Objects.requireNonNull(response.getResponse().getHeader("Location"));
        var invoiceId = StringUtils.substringAfterLast(location, "/");

        mockMvc.perform(put("/invoices/{id}", invoiceId)
                .with(jwtWithClaims("user-id-8", "alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "number": "999",
                            "auditing": {
                                "created_by": {
                                    "id": "666",
                                    "name": "evil"
                                },
                                "created_date" : "2001-01-01T01:01:01.001Z",
                                "last_modified_by": "someone else",
                                "last_modified_date" : "2001-01-01T01:01:01.001Z"
                            }
                        }
                        """)
        ).andExpect(status().isNoContent());

        assertThat(invoices.findById(UUID.fromString(invoiceId)))
                .isPresent()
                .hasValueSatisfying(invoice -> assertThat(invoice.getAuditing()).satisfies(auditing -> {
                    assertThat(auditing.getCreatedBy().getId()).isEqualTo("user-id-2");
                    assertThat(auditing.getCreatedBy().getName()).isEqualTo("john");
                    assertThat(auditing.getCreatedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getCreatedDate()).isEqualTo(created);

                    assertThat(auditing.getLastModifiedBy().getId()).isEqualTo("user-id-8");
                    assertThat(auditing.getLastModifiedBy().getName()).isEqualTo("alice");
                    assertThat(auditing.getLastModifiedBy().getNamespace()).isEqualTo(JWT_ISSUER_NAMESPACE);
                    assertThat(auditing.getLastModifiedDate()).isEqualTo(modified);
                }));


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
    @EnableJpaAuditing(dateTimeProviderRef = "mockedDateTimeProvider")
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