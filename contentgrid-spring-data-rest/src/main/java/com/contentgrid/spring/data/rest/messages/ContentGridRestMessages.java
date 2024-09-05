package com.contentgrid.spring.data.rest.messages;

import java.nio.charset.StandardCharsets;
import org.springframework.context.support.ResourceBundleMessageSource;

class ContentGridRestMessages extends ResourceBundleMessageSource {

    public ContentGridRestMessages() {
        setBasenames("com.contentgrid.spring.data.rest.messages");
        setDefaultEncoding(StandardCharsets.UTF_8.toString());
    }

}
