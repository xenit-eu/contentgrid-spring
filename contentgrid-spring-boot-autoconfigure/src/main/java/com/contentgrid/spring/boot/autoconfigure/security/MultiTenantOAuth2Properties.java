package com.contentgrid.spring.boot.autoconfigure.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contentgrid.security.oauth2")
public class MultiTenantOAuth2Properties {

    @Getter
    private final List<String> trustedJwtIssuers = new ArrayList<>();

}


