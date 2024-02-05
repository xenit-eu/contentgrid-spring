package com.contentgrid.spring.test.matchers;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EtagHeaderMatcher {

    public static String toEtag(int version) {
        return "\"%d\"".formatted(version);
    }

    public ResultMatcher exists() {
        return header().exists(HttpHeaders.ETAG);
    }

    public ResultMatcher isEqualTo(int expected) {
        return header().string(HttpHeaders.ETAG, toEtag(expected));
    }

    public ResultMatcher isNotEqualTo(int notExpected) {
        var matcher = not(toEtag(notExpected));
        return header().string(HttpHeaders.ETAG, matcher);
    }
}
