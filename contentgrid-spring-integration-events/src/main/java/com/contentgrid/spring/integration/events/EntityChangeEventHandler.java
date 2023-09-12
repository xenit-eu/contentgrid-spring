package com.contentgrid.spring.integration.events;

import org.springframework.messaging.MessageHandler;

@FunctionalInterface
public interface EntityChangeEventHandler {

    MessageHandler get();

}
