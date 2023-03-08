package com.contentgrid.spring.test.matchers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.net.URI;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LocationHeaderMatcher {

    public ResultMatcher uri(String url, Object... vars) {
        return header().string(HttpHeaders.LOCATION, initUri(url, vars).toString());
    }

    public ResultMatcher matches(@NonNull String template) {

        return header().string(HttpHeaders.LOCATION, new UriTemplateMatcher(template));
    }

    public ResultMatcher path(@NonNull String template) {
        return header().string(HttpHeaders.LOCATION, new UriTemplatePathMatcher(template));
    }

    private static URI initUri(String url, Object[] vars) {
        Assert.notNull(url, "'url' must not be null");
        Assert.isTrue(url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://"), "" +
                "'url' should start with a path or be a complete HTTP URL: " + url);
        return UriComponentsBuilder.fromUriString(url).buildAndExpand(vars).encode().toUri();
    }
}
