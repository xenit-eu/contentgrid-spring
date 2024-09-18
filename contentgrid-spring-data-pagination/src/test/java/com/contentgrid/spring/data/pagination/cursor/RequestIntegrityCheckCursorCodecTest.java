package com.contentgrid.spring.data.pagination.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorDecodeException;
import com.contentgrid.spring.data.pagination.cursor.RequestIntegrityCheckCursorCodec.IntegrityCheckFailedException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

class RequestIntegrityCheckCursorCodecTest {

    public static Stream<Arguments> withoutCursorParams() {
        return Stream.of(
                Arguments.of(new CursorContext(null, 5, Sort.unsorted()), "http://localhost/test?a=b")
        );
    }

    public static Stream<Arguments> testParams() {
        return Stream.of(
                Arguments.of(new CursorContext("abc", 1, Sort.unsorted()), "http://localhost/test?a=b"),
                Arguments.of(new CursorContext("abc", 1, Sort.unsorted()), "http://localhost/test"),
                Arguments.of(new CursorContext("ZZZ", 100, Sort.unsorted()), "http://xyz.example/test"),
                Arguments.of(new CursorContext("ZZZ", 100, Sort.by("abc")), "http://xyz.example/")
        );
    }

    @ParameterizedTest
    @MethodSource({"withoutCursorParams", "testParams"})
    void generateAndVerify(CursorContext cursorContext, String requestUri) throws CursorDecodeException {
        CursorCodec mockCodec = Mockito.mock(CursorCodec.class);

        var pageable = PageRequest.of(1,
                1); // pageable is always passed direct through to the delegate, so it's value is irrelevant
        var codec = new RequestIntegrityCheckCursorCodec(mockCodec);

        var requestUriComponents = UriComponentsBuilder.fromUriString(requestUri).build();

        Mockito.when(mockCodec.encodeCursor(pageable, requestUriComponents)).thenReturn(cursorContext);

        var encodedContext = codec.encodeCursor(pageable, requestUriComponents);

        Mockito.when(mockCodec.decodeCursor(cursorContext, requestUriComponents)).thenReturn(pageable);

        var decodedPageable = codec.decodeCursor(encodedContext, requestUriComponents);

        assertThat(decodedPageable).isEqualTo(pageable);
    }

    @ParameterizedTest
    @MethodSource("testParams")
    void modifyData(CursorContext cursorContext, String requestUri) throws CursorDecodeException {
        CursorCodec mockCodec = Mockito.mock(CursorCodec.class);

        var pageable = PageRequest.of(1,
                1); // pageable is always passed direct through to the delegate, so it's value is irrelevant
        var codec = new RequestIntegrityCheckCursorCodec(mockCodec);

        var requestUriComponents = UriComponentsBuilder.fromUriString(requestUri).build();

        Mockito.when(mockCodec.encodeCursor(pageable, requestUriComponents)).thenReturn(cursorContext);

        var encodedContext = codec.encodeCursor(pageable, requestUriComponents);

        Mockito.when(mockCodec.decodeCursor(cursorContext, requestUriComponents)).thenReturn(pageable);

        // Different ways of messing with the input data
        // - Change query parameter
        assertThatThrownBy(() -> {
            var modified = UriComponentsBuilder.fromUriString(requestUriComponents.toUriString())
                    .replaceQueryParam("a", "c")
                    .build();
            codec.decodeCursor(encodedContext, modified);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Add query parameter
        assertThatThrownBy(() -> {
            var modified = UriComponentsBuilder.fromUriString(requestUriComponents.toUriString())
                    .replaceQueryParam("x", "y")
                    .build();
            codec.decodeCursor(encodedContext, modified);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Remove query parameter
        if (requestUriComponents.getQueryParams().containsKey("a")) {
            assertThatThrownBy(() -> {
                var modified = UriComponentsBuilder.fromUriString(requestUriComponents.toUriString())
                        .replaceQueryParam("a")
                        .build();
                codec.decodeCursor(encodedContext, modified);
            }).isInstanceOf(IntegrityCheckFailedException.class);
        }

        // - Change path
        assertThatThrownBy(() -> {
            var modified = UriComponentsBuilder.fromUriString(requestUriComponents.toUriString())
                    .replacePath("xxx")
                    .build();
            codec.decodeCursor(encodedContext, modified);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Change page size
        assertThatThrownBy(() -> {
            var modifiedContext = new CursorContext(encodedContext.cursor(), encodedContext.pageSize() + 1,
                    encodedContext.sort());
            codec.decodeCursor(modifiedContext, requestUriComponents);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Append to cursor
        assertThatThrownBy(() -> {
            var modifiedContext = new CursorContext(encodedContext.cursor() + "ZZZ", encodedContext.pageSize(),
                    encodedContext.sort());
            codec.decodeCursor(modifiedContext, requestUriComponents);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Remove part from cursor
        assertThatThrownBy(() -> {
            var modifiedContext = new CursorContext(encodedContext.cursor().substring(5), encodedContext.pageSize(),
                    encodedContext.sort());
            codec.decodeCursor(modifiedContext, requestUriComponents);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Change sorting
        assertThatThrownBy(() -> {
            var modifiedContext = new CursorContext(encodedContext.cursor(), encodedContext.pageSize(),
                    encodedContext.sort().and(Sort.by("field")));
            codec.decodeCursor(modifiedContext, requestUriComponents);
        }).isInstanceOf(IntegrityCheckFailedException.class);

        // - Remove sorting
        if (!encodedContext.sort().isUnsorted()) {
            assertThatThrownBy(() -> {
                var modifiedContext = new CursorContext(encodedContext.cursor(), encodedContext.pageSize(),
                        Sort.unsorted());
                codec.decodeCursor(modifiedContext, requestUriComponents);
            }).isInstanceOf(IntegrityCheckFailedException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "https://localhost/abc", // protocol
            "http://localhost:123/abc", // port
            "http://example.test/abc", // host
            "http://a@localhost/abc", // userinfo
    })
    void doesNotCareAboutHostPart(String newUrl) throws CursorDecodeException {
        CursorCodec mockCodec = Mockito.mock(CursorCodec.class);
        CursorCodec.CursorContext cursorContext = new CursorContext("abc", 1, Sort.unsorted());

        var pageable = PageRequest.of(1,
                1); // pageable is always passed direct through to the delegate, so it's value is irrelevant
        var codec = new RequestIntegrityCheckCursorCodec(mockCodec);

        var encodeUriComponents = UriComponentsBuilder.fromUriString("http://localhost/abc").build();

        Mockito.when(mockCodec.encodeCursor(pageable, encodeUriComponents)).thenReturn(cursorContext);
        var encodedContext = codec.encodeCursor(pageable, encodeUriComponents);

        var decodeUriComponents = UriComponentsBuilder.fromUriString(newUrl).build();
        Mockito.when(mockCodec.decodeCursor(cursorContext, decodeUriComponents)).thenReturn(pageable);

        var decodedPageable = codec.decodeCursor(encodedContext, decodeUriComponents);

        assertThat(decodedPageable).isEqualTo(pageable);
    }

    @Test
    void extended_whenShortCrc() {
        CursorCodec codec = new RequestIntegrityCheckCursorCodec(new CursorCodec() {
            @Override
            public Pageable decodeCursor(CursorContext context, UriComponents uriComponents) {
                throw new UnsupportedOperationException("Test implementation can not decode cursors");
            }

            @Override
            public CursorContext encodeCursor(Pageable pageable, UriComponents uriComponents) {
                return CursorContext.builder()
                        .cursor("1")
                        .pageSize(pageable.getPageSize())
                        .sort(pageable.getSort())
                        .build();
            }
        });

        CursorCodec.CursorContext context = null;

        for (int i = 0; i < 10_000; i++) {
            var uriComponentsBuilder = UriComponentsBuilder.fromUriString("http://localhost/suppliers");
            uriComponentsBuilder.replaceQueryParam("t", Integer.toString(i));

            context = codec.encodeCursor(PageRequest.of(1, 10), uriComponentsBuilder.build());

            if (context.cursor().startsWith("0")) {
                break;
            }
        }

        assertThat(context)
                .isNotNull()
                .extracting(CursorContext::cursor)
                .asString()
                .startsWith("0");

    }
}