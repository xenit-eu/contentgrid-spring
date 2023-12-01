package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration(before = { OAuth2ResourceServerAutoConfiguration.class, SecurityAutoConfiguration.class })
@EnableConfigurationProperties(MultiTenantOAuth2Properties.class)
@ConditionalOnClass(value = {JwtIssuerAuthenticationManagerResolver.class, SecurityFilterChain.class, HttpSecurity.class})
@ConditionalOnWebApplication(type = Type.SERVLET)
class MultiTenantOAuth2ResourceServerAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("contentgrid.security.oauth2.trusted-jwt-issuers[0]")
    JwtIssuerAuthenticationManagerResolver authenticationManagerResolver(MultiTenantOAuth2Properties oauth2Properties) {
        return new JwtIssuerAuthenticationManagerResolver(oauth2Properties.getTrustedJwtIssuers());
    }

    @Bean
    @ConditionalOnBean(JwtIssuerAuthenticationManagerResolver.class)
    SecurityFilterChain jwtIssuerSecurityFilterChain(HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> authManagerResolver) throws Exception {
        http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());

        // contentgrid multi-jwt issuer-uris
        http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authManagerResolver));

        return http.build();
    }
//
//    @ConditionalOnBean(AuthenticationManagerResolver.class)
//    @Conditional(OAuth2ResourceServerJwtConfigurationAvailable.class)
//    @Configuration
//    static class Guard {
//
//        @PostConstruct
//        void verifyDuplicate() {
//            throw new RuntimeException("Configuration properties 'contentgrid.security.oauth2' and 'spring.security.oauth2.resourceserver.jwt' cannot be used at the same time");
//        }
//    }

//    static class OAuth2ResourceServerJwtConfigurationAvailable extends AnyNestedCondition {
//
//        OAuth2ResourceServerJwtConfigurationAvailable() {
//            super(ConfigurationPhase.REGISTER_BEAN);
//        }
//
//        @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
//        static class JwkKeySetUriPropertiesAvailable {
//
//        }
//
//        @Conditional(KeyValueCondition.class)
//        static class JwtPublicKeyValuePropertiesAvailable {
//
//        }
//
//        @Conditional(IssuerUriCondition.class)
//        static class IssuerUriPropertiesAvailable {
//
//        }
//
//    }
}
