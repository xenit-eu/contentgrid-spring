package com.contentgrid.spring.data.rest.messages;

import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

@Configuration(proxyBeanMethods = false)
public class ContentGridRestMessagesConfiguration {

    /**
     * Add fallback messages from ContentGridRestMessages to the spring-hateoas MessageResolver This allows users to
     * override or extend default messages using rest-messages.properties, while not running into trouble because there
     * are multiple rest-default-messages.properties files
     */
    @Bean
    static BeanPostProcessor contentGridHateoasRestMessagesPostProcessor(
            @Lazy ContentGridRestMessages restMessages
    ) {
        return new HateoasMessageResolverBeanPostProcessor(restMessages);
    }

    @Bean
    ContentGridRestMessages contentGridRestMessages() {
        return new ContentGridRestMessages();
    }

    @RequiredArgsConstructor
    private static class HateoasMessageResolverBeanPostProcessor implements BeanPostProcessor {

        private static final Class<?> RESOLVER_CLASS = ClassUtils.resolveClassName(
                "org.springframework.hateoas.mediatype.MessageSourceResolver", null);
        private static final Field RESOLVER_MESSAGE_SOURCE_ACCESSOR_FIELD = ReflectionUtils.findField(RESOLVER_CLASS,
                null, MessageSourceAccessor.class);

        private static final Field MESSAGE_SOURCE_ACCESSOR_MESSAGE_SOURCE_FIELD = ReflectionUtils.findField(
                MessageSourceAccessor.class, null, MessageSource.class);

        static {
            ReflectionUtils.makeAccessible(RESOLVER_MESSAGE_SOURCE_ACCESSOR_FIELD);
            ReflectionUtils.makeAccessible(MESSAGE_SOURCE_ACCESSOR_MESSAGE_SOURCE_FIELD);
        }

        private final ContentGridRestMessages contentGridRestMessages;

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof MessageResolver messageResolver) {
                var messageSource = extractMessageSource(messageResolver);
                injectParentMessageSource(messageSource, contentGridRestMessages);
            }
            return bean;
        }

        private MessageSource extractMessageSource(MessageResolver resolver) {
            if (RESOLVER_CLASS.isInstance(resolver)) {
                var accessor = (MessageSourceAccessor) ReflectionUtils.getField(RESOLVER_MESSAGE_SOURCE_ACCESSOR_FIELD,
                        resolver);
                return (MessageSource) ReflectionUtils.getField(MESSAGE_SOURCE_ACCESSOR_MESSAGE_SOURCE_FIELD, accessor);
            } else {
                return null;
            }
        }

        private void injectParentMessageSource(MessageSource messageSource, AbstractMessageSource parent) {
            if (messageSource instanceof AbstractMessageSource abstractMessageSource) {
                var currentParent = abstractMessageSource.getParentMessageSource();
                parent.setParentMessageSource(currentParent);
                abstractMessageSource.setParentMessageSource(parent);
            }
        }

    }

}
