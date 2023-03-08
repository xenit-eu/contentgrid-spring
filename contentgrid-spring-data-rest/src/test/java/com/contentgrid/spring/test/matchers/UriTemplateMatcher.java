package com.contentgrid.spring.test.matchers;

import java.net.URI;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.springframework.web.util.UriTemplate;

@RequiredArgsConstructor
public class UriTemplateMatcher extends BaseMatcher<String> {

    @Getter(AccessLevel.PROTECTED)
    private final UriTemplate template;

    public UriTemplateMatcher(String template) {
        this.template = new UriTemplate(template);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("uriMatches(").appendValue(template).appendText(")");
    }

    @Override
    public boolean matches(Object actual) {
        if (actual == null) {
            return false;
        }
        try {
            var uri = URI.create(actual.toString());
            return this.template.matches(uri.toString());
        } catch (IllegalArgumentException ex) {
            return false;
        }

    }
}
