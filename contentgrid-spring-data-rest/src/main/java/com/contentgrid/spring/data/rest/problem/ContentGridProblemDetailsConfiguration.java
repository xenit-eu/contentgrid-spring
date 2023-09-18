package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.PlainMapping;
import java.util.Locale;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.MessageSourceSupport;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableHypermediaSupport(type = HypermediaType.HTTP_PROBLEM_DETAILS)
public class ContentGridProblemDetailsConfiguration {

    private final ApplicationContext applicationContext;

    @Bean
    ProblemTypeUrlFactory contentGridProblemTypeUrlFactory() {
        return new ProblemTypeUrlFactory(UriTemplate.of("https://contentgrid.com/rels/problem{/item*}"));
    }

    @Bean
    JsonPropertyPathConverter jsonPropertyPathConverter(@PlainMapping DomainTypeMapping domainTypeMapping) {
        return new JsonPropertyPathConverter(domainTypeMapping);
    }

    @Bean
    ProblemFactory problemFactory(ProblemTypeUrlFactory problemTypeUrlFactory) {
        return new ProblemFactory(ProblemTypeMessageSource.getAccessor(), problemTypeUrlFactory);
    }

    @Bean
    @Order(-1)
    ContentGridExceptionHandler contentGridExceptionHandler(
            ProblemFactory problemFactory,
            JsonPropertyPathConverter jsonPropertyPathConverter) {
        return new ContentGridExceptionHandler(
                problemFactory,
                new MessageSourceAccessor(applicationContext),
                jsonPropertyPathConverter
        );
    }

    @Bean
    @Order(0)
    SpringDataRestRepositoryExceptionHandler contentGridSpringDataRestRepositoryExceptionHandler() {
        return new SpringDataRestRepositoryExceptionHandler();
    }

    @Bean
    @Order(0)
    SpringContentRestExceptionHandler contentGridSpringContentRestRepositoryExceptionHandler() {
        return new SpringContentRestExceptionHandler();
    }

    @RequiredArgsConstructor
    private static class DelegatingMessageSource extends MessageSourceSupport implements MessageSource {

        private final MessageSource primary;
        private final MessageSource fallback;

        @Override
        public String getMessage(@NonNull String code, Object[] args, String defaultMessage, @NonNull Locale locale) {
            var message = primary.getMessage(code, args, null, locale);
            if (message != null) {
                return message;
            }
            return fallback.getMessage(code, args, defaultMessage, locale);
        }

        @Override
        @NonNull
        public String getMessage(@NonNull String code, Object[] args, @NonNull Locale locale)
                throws NoSuchMessageException {
            try {
                return primary.getMessage(code, args, locale);
            } catch (NoSuchMessageException ex) {
                return fallback.getMessage(code, args, locale);
            }
        }

        @Override
        @NonNull
        public String getMessage(@NonNull MessageSourceResolvable resolvable, @NonNull Locale locale)
                throws NoSuchMessageException {
            try {
                return primary.getMessage(resolvable, locale);
            } catch (NoSuchMessageException ex) {
                return fallback.getMessage(resolvable, locale);
            }
        }
    }

}
