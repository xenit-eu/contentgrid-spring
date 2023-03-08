package com.contentgrid.spring.test.matchers;

import org.springframework.test.web.servlet.result.HeaderResultMatchers;

public class ExtendedHeaderResultMatchers extends HeaderResultMatchers {

    public static ExtendedHeaderResultMatchers headers() {
        return new ExtendedHeaderResultMatchers();
    }

    public LocationHeaderMatcher location() {
        return new LocationHeaderMatcher();
    }
}
