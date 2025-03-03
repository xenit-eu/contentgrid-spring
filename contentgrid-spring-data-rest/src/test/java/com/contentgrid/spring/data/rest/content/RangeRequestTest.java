package com.contentgrid.spring.data.rest.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "contentgrid.security.unauthenticated.allow = true",
                "contentgrid.security.csrf.disabled = true",
                "spring.content.storage.type.default = fs",
                "contentgrid.thunx.abac.source = none",
        })
public class RangeRequestTest {

    private static final String TEXT = "Hello world!";
    private static final byte[] CONTENT = TEXT.getBytes(StandardCharsets.UTF_8);
    private static final String FILENAME = "hello.txt";

    private String contentUrl;

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setup() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:%s".formatted(port))
                .responseTimeout(Duration.ofMinutes(10)) // For debugging
                .build();

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ByteArrayResource(CONTENT), MediaType.TEXT_PLAIN)
                .filename(FILENAME);
        multipartBodyBuilder.part("name", "test");

        // Upload content
        var response = client.post()
                .uri("/persons", port)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class);

        contentUrl = response.getResponseHeaders().getLocation() + "/file";
    }

    @Test
    void rangeRequest_http206() {
        var start = 5;
        var end = 9;

        var expected = Arrays.copyOfRange(CONTENT, start, end + 1);

        client.get().uri(contentUrl)
                .accept(MediaType.ALL)
                .header(HttpHeaders.RANGE, "bytes=%s-%s".formatted(start, end))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT)
                .expectBody(byte[].class).isEqualTo(expected);
    }

    @Test
    void rangeRequest_upToLastByte_http206() {
        var start = 5;
        var end = CONTENT.length;
        var expected = Arrays.copyOfRange(CONTENT, start, end);

        client.get().uri(contentUrl)
                .accept(MediaType.ALL)
                .header(HttpHeaders.RANGE, "bytes=%s-".formatted(start))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT)
                .expectBody(byte[].class).isEqualTo(expected);
    }

    @Test
    void rangeRequest_lastNBytes_http206() {
        var length = 5;
        var end = CONTENT.length;
        var expected = Arrays.copyOfRange(CONTENT, end - length, end);

        client.get().uri(contentUrl)
                .accept(MediaType.ALL)
                .header(HttpHeaders.RANGE, "bytes=-%s".formatted(length))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT)
                .expectBody(byte[].class).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "50,54", // start > length
            "10,9",  // start > end
            "-1,9",  // start < 0
    })
    void invalidRangeRequest_http416(int start, int end) {
        client.get().uri(contentUrl)
                .accept(MediaType.ALL)
                .header(HttpHeaders.RANGE, "bytes=%s-%s".formatted(start, end))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true)
    static class TestApp {

    }

    @Entity
    @NoArgsConstructor
    @Getter
    @Setter
    static class Person {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private UUID id;

        private String name;

        @Embedded
        @AttributeOverride(name = "id", column = @Column(name = "file__id"))
        @AttributeOverride(name = "length", column = @Column(name = "file__length"))
        @AttributeOverride(name = "mimetype", column = @Column(name = "file__mimetype"))
        @AttributeOverride(name = "filename", column = @Column(name = "file__filename"))
        @RestResource(linkRel = "d:file", path = "file")
        private Content file;
    }

    @Embeddable
    @Getter
    @Setter
    static class Content {

        @ContentId
        @JsonIgnore
        private String id;

        @ContentLength
        @JsonProperty(access = Access.READ_ONLY)
        private Long length;

        @MimeType
        private String mimetype;

        @OriginalFileName
        private String filename;
    }

    @RepositoryRestResource(collectionResourceRel = "d:persons", itemResourceRel = "d:person")
    interface PersonRepository extends JpaRepository<Person, UUID>, QuerydslPredicateExecutor<Person> {

    }

    @StoreRestResource
    interface PersonContentStore extends ContentStore<Person, String> {

    }

    @Configuration
    static class RestConfiguration implements RepositoryRestConfigurer {

        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config,
                CorsRegistry cors) {
            config.exposeIdsFor(Person.class);
        }
    }
}
