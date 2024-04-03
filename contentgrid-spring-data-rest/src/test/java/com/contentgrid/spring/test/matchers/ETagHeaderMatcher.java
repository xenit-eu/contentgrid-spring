package com.contentgrid.spring.test.matchers;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ETagHeaderMatcher {

    public static String toETag(int version) {
        return "\"%d\"".formatted(version);
    }

    public ResultMatcher exists() {
        return header().exists(HttpHeaders.ETAG);
    }

    public ResultMatcher isEqualTo(String expected) {
        return header().string(HttpHeaders.ETAG, expected);
    }

    public ResultMatcher isNotEqualTo(String notExpected) {
        var matcher = not(notExpected);
        return header().string(HttpHeaders.ETAG, matcher);
    }
}
