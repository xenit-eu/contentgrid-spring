package com.contentgrid.spring.data.rest.problem;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.assertj.core.api.ThrowingConsumer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProblemDetailsMockMvcMatchers {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    }

    public static ProblemDetailsMatcher problemDetails() {
        return new ProblemDetailsMatcher();
    }

    public static ValidationConstraintViolationMatcher validationConstraintViolation() {
        return new ValidationConstraintViolationMatcher(List.of());
    }

    @With
    @AllArgsConstructor
    public static class ProblemDetailsMatcher implements ResultMatcher {

        private final static String SENTINEL = "\0";
        private final String type;
        private final String title;
        private final HttpStatusCode statusCode;

        public ProblemDetailsMatcher() {
            this(SENTINEL, SENTINEL, null);
        }

        ProblemDetail readProblemDetail(MvcResult result) throws IOException {
            assertThat(result.getResponse().getContentType())
                    .as("response Content-Type")
                    .isEqualTo("application/problem+json");

            var problemDetails = objectMapper.reader()
                    .readValue(result.getResponse().getContentAsByteArray(), ProblemDetail.class);

            if (statusCode != null) {
                assertThat(result.getResponse().getStatus())
                        .as("response status code")
                        .isEqualTo(statusCode.value());
                assertThat(problemDetails)
                        .extracting(ProblemDetail::getStatus)
                        .as("problem status")
                        .isEqualTo(statusCode.value());
            }

            if (!Objects.equals(type, SENTINEL)) {
                assertThat(problemDetails)
                        .extracting(ProblemDetail::getType)
                        .extracting(URI::toString)
                        .as("problem type")
                        .isEqualTo(type);
            }
            if (!Objects.equals(title, SENTINEL)) {
                assertThat(problemDetails)
                        .extracting(ProblemDetail::getTitle)
                        .as("problem title")
                        .isEqualTo(title);
            }

            return problemDetails;
        }

        @Override
        public void match(MvcResult result) throws Exception {
            readProblemDetail(result);
        }
    }

    @AllArgsConstructor
    public static class ValidationConstraintViolationMatcher implements ResultMatcher {

        private final static ProblemDetailsMatcher PROBLEM_DETAILS_MATCHER = new ProblemDetailsMatcher()
                .withStatusCode(HttpStatus.BAD_REQUEST)
                .withType("https://contentgrid.cloud/problems/integrity/validation-constraint-violation");

        @With(AccessLevel.PRIVATE)
        private final List<ErrorDescription> errors;

        public ValidationConstraintViolationMatcher withError(ErrorDescription description) {
            return withErrors(Stream.concat(
                    errors.stream(),
                    Stream.of(description)
            ).toList());
        }

        public ValidationConstraintViolationMatcher withError(UnaryOperator<ErrorDescription> configurer) {
            return withError(configurer.apply(new ErrorDescription()));
        }

        @Override
        public void match(MvcResult result) throws Exception {
            var details = PROBLEM_DETAILS_MATCHER.readProblemDetail(result);
            var properties = details.getProperties();
            assertThat(properties).containsKey("errors")
                    .extractingByKey("errors")
                    .isInstanceOf(List.class);

            var errors = (List) properties.get("errors");

            assertThat(errors)
                    .satisfiesExactlyInAnyOrder(
                            this.errors.stream().map(ErrorDescription::toSatisfies).toArray(ThrowingConsumer[]::new));

        }

        @RequiredArgsConstructor
        public static class ErrorDescription {

            private final static ThrowingConsumer<String> SENTINEL = (s) -> {
            };
            private final ThrowingConsumer<String> type;
            private final ThrowingConsumer<String> title;
            private final ThrowingConsumer<String> property;

            public ErrorDescription() {
                this(SENTINEL, SENTINEL, SENTINEL);
            }

            public ErrorDescription withType(ThrowingConsumer<String> type) {
                return new ErrorDescription(type, this.title, this.property);
            }

            public ErrorDescription withTitle(ThrowingConsumer<String> title) {
                return new ErrorDescription(this.type, title, this.property);
            }

            public ErrorDescription withProperty(ThrowingConsumer<String> property) {
                return new ErrorDescription(this.type, this.title, property);
            }

            public ErrorDescription withType(String value) {
                return withType(t -> assertThat(t).isEqualTo(value));
            }

            public ErrorDescription withTitle(String value) {
                return withTitle(t -> assertThat(t).isEqualTo(value));
            }

            public ErrorDescription withProperty(String value) {
                return withProperty(t -> assertThat(t).isEqualTo(value));
            }

            ThrowingConsumer<Map> toSatisfies() {
                return (data) -> {
                    assertThat(data)
                            .extractingByKey("property")
                            .satisfies(property);

                    assertThat(data)
                            .extractingByKey("type")
                            .satisfies(type);

                    assertThat(data)
                            .extractingByKey("title")
                            .satisfies(title);
                };

            }
        }
    }
}
