package com.contentgrid.spring.test.matchers;

import java.net.URI;
import lombok.NonNull;
import org.hamcrest.Description;
import org.springframework.web.util.UriTemplate;

public class UriTemplatePathMatcher extends UriTemplateMatcher {

    public UriTemplatePathMatcher(UriTemplate template) {
        super(template);
    }

    public UriTemplatePathMatcher(String template) {
        super(template);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("pathMatches(").appendValue(this.getTemplate().toString()).appendText(")");
    }

    @Override
    public boolean matches(@NonNull Object actual) {
        try {
            var uri = URI.create(actual.toString());
            return super.matches(uri.getRawPath());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
