package com.contentgrid.spring.data.pagination.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.pagination.InvalidPageSizeException;
import com.contentgrid.spring.data.pagination.InvalidPaginationException;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorDecodeException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriComponentsBuilder;

class HateoasPageableCursorHandlerMethodArgumentResolverTest {

    CursorCodec cursorCodec;

    private HateoasPageableCursorHandlerMethodArgumentResolver createHandlerMethodArgumentResolver() {
        return new HateoasPageableCursorHandlerMethodArgumentResolver(
                new HateoasSortHandlerMethodArgumentResolver(),
                cursorCodec,
                List.of()
        );
    }

    private static Answer<Pageable> answerFromCursor(Function<CursorContext, Pageable> fn) {
        return invocation -> {
            var ctx = invocation.getArgument(0, CursorContext.class);
            return fn.apply(ctx);
        };
    }

    private static ServletWebRequest createRequest(Consumer<MockHttpServletRequest> servletRequestConsumer) {
        var mockRequest = new MockHttpServletRequest();
        servletRequestConsumer.accept(mockRequest);
        return new ServletWebRequest(mockRequest);
    }

    @BeforeEach
    void setUp() throws CursorDecodeException {
        cursorCodec = Mockito.mock(CursorCodec.class);
        Mockito.when(cursorCodec.decodeCursor(Mockito.any()))
                .thenAnswer(answerFromCursor(ctx -> PageRequest.ofSize(ctx.pageSize())
                        .withSort(ctx.sort())
                ));
    }

    @Test
    void pageableWithoutParameters() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.resolveArgument(Sample.SUPPORTED_METHOD, null, createRequest(w -> {
        }), null);

        Mockito.verify(cursorCodec).decodeCursor(new CursorContext(null, 20, Sort.unsorted()));
        Mockito.verifyNoMoreInteractions(cursorCodec);
    }

    @Test
    void pageableWithSize() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.resolveArgument(Sample.SUPPORTED_METHOD, null, createRequest(req -> {
            req.addParameter("size", "50");
        }), null);

        Mockito.verify(cursorCodec).decodeCursor(new CursorContext(null, 50, Sort.unsorted()));
        Mockito.verifyNoMoreInteractions(cursorCodec);
    }

    @Test
    void pageableWithOverlyLargeSize() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.setMaxPageSize(100);

        resolver.resolveArgument(Sample.SUPPORTED_METHOD, null, createRequest(req -> {
            req.addParameter("size", "5000");
        }), null);

        Mockito.verify(cursorCodec).decodeCursor(new CursorContext(null, 100, Sort.unsorted()));
        Mockito.verifyNoMoreInteractions(cursorCodec);

    }

    @Test
    void pageableWithSort() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.resolveArgument(Sample.SUPPORTED_METHOD, null, createRequest(req -> {
            req.addParameter("sort", "xyz,desc", "abc,asc");
        }), null);

        Mockito.verify(cursorCodec)
                .decodeCursor(new CursorContext(null, 20, Sort.by(Order.desc("xyz"), Order.asc("abc"))));
        Mockito.verifyNoMoreInteractions(cursorCodec);
    }

    @Test
    void pageableWithPage() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.resolveArgument(Sample.SUPPORTED_METHOD, null, createRequest(req -> {
            req.addParameter("page", "my-value", "my-second-value");
        }), null);

        Mockito.verify(cursorCodec).decodeCursor(new CursorContext("my-value", 20, Sort.unsorted()));
        Mockito.verifyNoMoreInteractions(cursorCodec);
    }

    @Test
    void pageableWithDefaultAnnotation() throws CursorDecodeException {
        var resolver = createHandlerMethodArgumentResolver();

        resolver.resolveArgument(Sample.DEFAULT_PAGEABLE, null, createRequest(req -> {
            req.addParameter("page", "my-value");
        }), null);

        Mockito.verify(cursorCodec).decodeCursor(new CursorContext("my-value", 10, Sort.unsorted()));
        Mockito.verifyNoMoreInteractions(cursorCodec);
    }

    @Test
    void pageableUnparsableSize() {
        var resolver = createHandlerMethodArgumentResolver();

        assertThatThrownBy(() -> {
            resolver.resolveArgument(Sample.DEFAULT_PAGEABLE, null, createRequest(req -> {
                req.addParameter("size", "non-numeric");
            }), null);
        }).isInstanceOfSatisfying(InvalidPageSizeException.class, ex -> {
            assertThat(ex).hasCauseInstanceOf(NumberFormatException.class);
            assertThat(ex).hasMessageStartingWith("Invalid parameter 'size': ");
        });
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "-1"
    })
    void pageableInvalidSize(String sizeParam) {
        var resolver = createHandlerMethodArgumentResolver();

        assertThatThrownBy(() -> {
            resolver.resolveArgument(Sample.DEFAULT_PAGEABLE, null, createRequest(req -> {
                req.addParameter("size", sizeParam);
            }), null);
        }).isInstanceOfSatisfying(InvalidPageSizeException.class, ex -> {
            assertThat(ex).hasMessageStartingWith("Invalid parameter 'size': must be positive");
        });
    }

    @Test
    void pageableCursorException() throws CursorDecodeException {
        Mockito.doThrow(new CursorDecodeException("failed to decode")).when(cursorCodec).decodeCursor(Mockito.any());

        var resolver = createHandlerMethodArgumentResolver();

        assertThatThrownBy(() -> {
            resolver.resolveArgument(Sample.DEFAULT_PAGEABLE, null, createRequest(req -> {
                req.addParameter("page", "some-invalid-value");
            }), null);
        }).isInstanceOfSatisfying(InvalidPaginationException.class, ex -> {
            assertThat(ex).hasCauseInstanceOf(CursorDecodeException.class);
            assertThat(ex).hasMessageStartingWith("Invalid parameter 'page': failed to decode");
        });
    }

    @Test
    void fillsTemplateParameters() {
        var pageRequest = PageRequest.of(1, 13, Sort.by("abc"));
        Mockito.when(cursorCodec.encodeCursor(pageRequest))
                .thenReturn(new CursorContext("encoded-cursor", 15, Sort.unsorted()));

        var resolver = createHandlerMethodArgumentResolver();

        var builder = UriComponentsBuilder.newInstance();
        resolver.enhance(builder, Sample.DEFAULT_PAGEABLE, pageRequest);

        assertThat(builder.build().getQueryParams())
                .containsEntry("page", List.of("encoded-cursor"))
                .containsEntry("size", List.of("15"))
                .doesNotContainKey("sort");
    }

    @Test
    void fillsTemplateParametersWithSort() {
        var pageRequest = PageRequest.of(1, 13, Sort.by("abc"));
        Mockito.when(cursorCodec.encodeCursor(pageRequest))
                .thenReturn(new CursorContext("encoded-cursor", 15, Sort.by("abc")));

        var resolver = createHandlerMethodArgumentResolver();

        var builder = UriComponentsBuilder.newInstance();
        resolver.enhance(builder, Sample.DEFAULT_PAGEABLE, pageRequest);

        assertThat(builder.build().getQueryParams())
                .containsEntry("page", List.of("encoded-cursor"))
                .containsEntry("size", List.of("15"))
                .containsEntry("sort", List.of("abc,asc"));
    }

    abstract static class Sample {

        public static final MethodParameter SUPPORTED_METHOD;
        public static final MethodParameter DEFAULT_PAGEABLE;

        static {
            try {
                SUPPORTED_METHOD = MethodParameter.forExecutable(
                        Sample.class.getMethod("supportedMethod", Pageable.class), 0);
                DEFAULT_PAGEABLE = MethodParameter.forExecutable(
                        Sample.class.getMethod("defaultPageable", Pageable.class), 0);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public abstract void supportedMethod(Pageable pageable);

        public abstract void defaultPageable(@PageableDefault Pageable pageable);
    }

}