package com.contentgrid.spring.test.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.StringUtils;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockJwt.WithMockJwtSecurityContextFactory.class)
@Import({SkipCsrfConfiguration.class})
public @interface WithMockJwt {
    @AliasFor("subject")
    String value() default "user";

    @AliasFor("value")
    String subject() default "user";

    String name() default "";

    String issuer() default "http://localhost/realms/0";

    class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwt> {

        @Override
        public SecurityContext createSecurityContext(WithMockJwt annotation) {
            Jwt.Builder builder = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(annotation.subject())
                    .issuer(annotation.issuer())
                    .claim("scope", "read");

            if (StringUtils.hasLength(annotation.name())) {
                builder.claim("name", annotation.name());
            }
            Jwt jwt = builder.build();

            List<GrantedAuthority> grantedAuthorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));
            Authentication authentication = new JwtAuthenticationToken(jwt, grantedAuthorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            return context;
        }
    }
}
