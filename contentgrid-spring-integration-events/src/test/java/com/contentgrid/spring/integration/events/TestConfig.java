package com.contentgrid.spring.integration.events;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

@TestConfiguration(proxyBeanMethods = false)
public class TestConfig {

    @Bean
    TestMessageHandler mockMessageHandler() {
        return new TestMessageHandler();
    }

    @Bean
    EntityChangeEventHandler testEventHandler(TestMessageHandler testMessageHandler) {
        return () -> testMessageHandler;
    }

    public static class TestMessageHandler extends AbstractMessageHandler {

        private final Deque<Message<?>> messages = new LinkedList<>();

        @Override
        public void destroy() {
            reset();
            super.destroy();
        }

        @Override
        protected void handleMessageInternal(Message<?> message) {
            messages.add(message);
        }

        public Stream<Message<?>> messages() {
            return messages.stream();
        }

        public Optional<Message<?>> lastMessage() {
            return Optional.ofNullable(messages.peekLast());
        }

        public void reset() {
            messages.clear();
        }
    }
}
