package com.contentgrid.spring.data.rest.links;

import lombok.Value;
import org.springframework.context.MessageSourceResolvable;

@Value
class LinkTitles implements MessageSourceResolvable {
    String[] codes;

    public static LinkTitles forEntity(Class<?> domainType) {
        return new LinkTitles(domainType.getName() + "._title");
    }

    public static LinkTitles forProperty(Class<?> domainType, String propertyName) {
        return new LinkTitles(domainType.getName() + "." + propertyName + "._title");
    }

    private LinkTitles(String... codes) {
        this.codes = codes;
    }

    @Override
    public String getDefaultMessage() {
        return "";
    }
}
