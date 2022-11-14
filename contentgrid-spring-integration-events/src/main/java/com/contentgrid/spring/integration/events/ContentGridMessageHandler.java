package com.contentgrid.spring.integration.events;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.messaging.MessageHandler;

@FunctionalInterface
public interface ContentGridMessageHandler {

    MessageHandlerSpec<?, ? extends MessageHandler> get();

}
