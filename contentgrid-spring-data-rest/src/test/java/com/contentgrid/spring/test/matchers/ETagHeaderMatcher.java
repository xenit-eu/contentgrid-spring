package com.contentgrid.spring.test.matchers;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ETagHeaderMatcher {

    public static ETag toETag(int version) {
        return ETag.from(String.valueOf(version));
    }

    public ResultMatcher exists() {
        return header().exists(HttpHeaders.ETAG);
    }

    public ResultMatcher isEqualTo(ETag expected) {
        return header().string(HttpHeaders.ETAG, expected.toString());
    }

    public ResultMatcher isNotEqualTo(ETag notExpected) {
        var matcher = not(notExpected.toString());
        return header().string(HttpHeaders.ETAG, matcher);
    }
}
