package com.contentgrid.spring.data.querydsl.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.data.querydsl.paths.PathNavigator;
import com.contentgrid.spring.data.querydsl.sort.CollectionFilterSortHandlerMethodArgumentResolverTest.LocalConfiguration;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.querydsl.predicate.None;
import com.contentgrid.spring.querydsl.predicate.Text;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.hateoas.server.mvc.UriComponentsContributor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(classes = {InvoicingApplication.class, LocalConfiguration.class})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class CollectionFilterSortHandlerMethodArgumentResolverTest {

    @Autowired
    private SortArgumentResolver sortArgumentResolver;

    @Autowired
    @Qualifier("sortResolver")
    private UriComponentsContributor uriComponentsContributor;

    @Autowired
    private CollectionFiltersMapping collectionFiltersMapping;

    private static final EntityPathResolver entityPathResolver = SimpleEntityPathResolver.INSTANCE;


    private static final MethodParameter FAKE_METHOD_PARAM;

    static {
        try {
            FAKE_METHOD_PARAM = MethodParameter.forExecutable(
                    CollectionFilterSortHandlerMethodArgumentResolverTest.class.getDeclaredMethod(
                            "functionForMethodParameter", Sort.class), 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is a stub that is used to create an {@link MethodParameter}: {@link #FAKE_METHOD_PARAM}
     */
    @RequestMapping("/{repository}")
    static void functionForMethodParameter(Sort sort) {
        // This function is never called, it is just a stub to obtain a MethodParameter
    }

    private Sort parseRequest(HttpServletRequest request) {
        return sortArgumentResolver.resolveArgument(
                FAKE_METHOD_PARAM,
                null,
                new ServletWebRequest(request),
                null
        );
    }

    private UriComponents buildComponents(HttpServletRequest request, Sort sort) {
        var builder = UriComponentsBuilder.fromUriString(request.getRequestURI());
        uriComponentsContributor.enhance(builder, FAKE_METHOD_PARAM, sort);
        return builder.build();
    }

    @Test
    void simpleStringSort() {
        var request = new MockHttpServletRequest("GET", "/entity-with-weird-filter-params");
        request.addParameter("sort", "simple_string");
        var sort = parseRequest(request);

        var pathNavigator = new PathNavigator(entityPathResolver.createPath(EntityWithWeirdFilterParams.class));

        assertThat(sort)
                .isInstanceOfSatisfying(QSortWithOriginalSort.class, qSort -> {
                    assertThat(qSort.getOriginalSort()).containsExactly(Sort.Order.by("simple_string"));
                    assertThat(qSort.getOrderSpecifiers()).containsExactly(new OrderSpecifier<>(
                            Order.ASC,
                            Expressions.stringTemplate("normalize({0s})", pathNavigator.get("simpleString").getPath())
                    ));
                });

        assertThat(buildComponents(request, sort).getQueryParams())
                .containsExactly(Map.entry("sort", List.of("simple_string,asc")));
    }

    @Test
    void ignoreCaseStringSort() {

        var request = new MockHttpServletRequest("GET", "/entity-with-weird-filter-params");
        request.addParameter("sort", "simple_string_ignorecase,desc");
        var sort = parseRequest(request);

        var pathNavigator = new PathNavigator(entityPathResolver.createPath(EntityWithWeirdFilterParams.class));

        assertThat(sort)
                .isInstanceOfSatisfying(QSortWithOriginalSort.class, qSort -> {
                    assertThat(qSort.getOriginalSort()).containsExactly(Sort.Order.desc("simple_string_ignorecase"));
                    assertThat(qSort.getOrderSpecifiers()).containsExactly(new OrderSpecifier<>(
                            Order.DESC,
                            Expressions.stringTemplate("normalize({0s})",
                                            pathNavigator.get("simpleString").getPath())
                                    .lower()
                    ));
                });
        assertThat(buildComponents(request, sort).getQueryParams())
                .containsExactly(Map.entry("sort", List.of("simple_string_ignorecase,desc")));
    }

    @Test
    void crossEmbeddedObjectSort() {
        var request = new MockHttpServletRequest("GET", "/customers");
        request.addParameter("sort", "content.size");
        var sort = parseRequest(request);

        assertThat(sort)
                .isInstanceOfSatisfying(QSortWithOriginalSort.class, qSort -> {
                    assertThat(qSort.getOriginalSort()).containsExactly(Sort.Order.asc("content.size"));
                    assertThat(qSort.getOrderSpecifiers()).containsExactly(new OrderSpecifier<>(
                            Order.ASC,
                            QCustomer.customer.content.length
                    ));
                });

        assertThat(buildComponents(request, sort).getQueryParams())
                .containsExactly(Map.entry("sort", List.of("content.size,asc")));
    }

    @ParameterizedTest
    @CsvSource({
            "myInteger",
            "myDecimal",
            "myInstant"
    })
    void comparableSort(String param) {

        var request = new MockHttpServletRequest("GET", "/entity-with-weird-filter-params");
        request.addParameter("sort", param);
        var sort = parseRequest(request);

        var pathNavigator = new PathNavigator(entityPathResolver.createPath(EntityWithWeirdFilterParams.class));

        assertThat(sort)
                .isInstanceOfSatisfying(QSortWithOriginalSort.class, qSort -> {
                    assertThat(qSort.getOriginalSort()).containsExactly(Sort.Order.asc(param));
                    assertThat(qSort.getOrderSpecifiers()).containsExactly(new OrderSpecifier<>(
                            Order.ASC,
                            (Expression<Comparable<?>>) pathNavigator.get(param).getPath()
                    ));
                });
    }

    @Test
    void unknownSortParameterError() {
        var request = new MockHttpServletRequest("GET", "/entity-with-weird-filter-params");
        request.addParameter("sort", "xyz");
        assertThatThrownBy(() -> parseRequest(request))
                .hasMessage("Sort parameter 'xyz' is not supported")
                .isInstanceOfSatisfying(UnsupportedSortPropertyException.class, ex -> {
                    assertThat(ex.getOrder()).isEqualTo(Sort.Order.asc("xyz"));
                });
    }

    @ParameterizedTest
    @CsvSource({
            "simple_string_starts_with", // Has no sort mapping
            "invoice.paid", // Is across a relation
            "invoice" // Has no sort mapping
    })
    void unsupportedSortParameterError(String param) {
        assertThat(collectionFiltersMapping.forDomainType(EntityWithWeirdFilterParams.class)
                .named(param))
                .describedAs(
                        "A collection filter parameter with name '%s' must exist, so it can be verified that sorting on it is rejected",
                        param)
                .isPresent();

        var request = new MockHttpServletRequest("GET", "/entity-with-weird-filter-params");
        request.addParameter("sort", param);
        assertThatThrownBy(() -> parseRequest(request))
                .hasMessage("Sort parameter '%s' is not supported".formatted(param))
                .isInstanceOfSatisfying(UnsupportedSortPropertyException.class, ex -> {
                    assertThat(ex.getOrder()).isEqualTo(Sort.Order.asc(param));
                });

    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {CollectionFilterSortHandlerMethodArgumentResolverTest.class,
            InvoicingApplication.class})
    @EnableJpaRepositories(basePackageClasses = {CollectionFilterSortHandlerMethodArgumentResolverTest.class,
            InvoicingApplication.class}, considerNestedRepositories = true)
    public static class LocalConfiguration {

    }

    @Entity
    public static class EntityWithWeirdFilterParams {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        @CollectionFilterParam(value = "simple_string", predicate = Text.EqualsNormalized.class)
        @CollectionFilterParam(value = "simple_string_ignorecase", predicate = Text.EqualsIgnoreCaseNormalized.class)
        @CollectionFilterParam(value = "simple_string_starts_with", predicate = Text.StartsWithIgnoreCaseNormalized.class)
        private String simpleString;

        @CollectionFilterParam
        private Integer myInteger;

        @CollectionFilterParam
        private BigDecimal myDecimal;

        @CollectionFilterParam
        private Instant myInstant;

        @OneToOne
        @CollectionFilterParam(predicate = EntityId.class)
        @CollectionFilterParam(predicate = None.class)
        private Invoice invoice;
    }

    @RepositoryRestResource(path = "entity-with-weird-filter-params")
    public interface EntityWithWeirdFilterParamsRepository extends
            JpaRepository<EntityWithWeirdFilterParams, UUID>,
            QuerydslPredicateExecutor<EntityWithWeirdFilterParams> {

    }

}