/*
 * Copyright 2020-Present The CloudEvents Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contentgrid.spring.cloudevents.amqp;

import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

/**
 * Internal utility class for copying <code>CloudEvent</code> context to a map (message headers).
 *
 * <p>
 * Modified from <a
 * href="https://github.com/cloudevents/sdk-java/tree/fb11b94f2b534858f698fce3b122fa6625f2d184/spring/src/main/java/io/cloudevents/spring/messaging">CloudEvents
 * Spring</a> to use the AMQP MessageConverter instead of spring integration messaging
 *
 * @author Dave Syer
 * @author Lars Vierbergen
 */
@RequiredArgsConstructor
class MessageBuilderMessageWriter
        implements CloudEventWriter<Message>, MessageWriter<MessageBuilderMessageWriter, Message> {

    private final MessageProperties messageProperties;

    @Override
    public Message setEvent(EventFormat format, byte[] value) throws CloudEventRWException {
        messageProperties.setContentType(format.serializedContentType());
        return MessageBuilder.withBody(value)
                .copyProperties(messageProperties)
                .build();
    }

    @Override
    public Message end(CloudEventData value) throws CloudEventRWException {
        return MessageBuilder.withBody(value == null ? new byte[0] : value.toBytes())
                .copyProperties(messageProperties)
                .build();
    }

    @Override
    public Message end() {
        return end(null);
    }

    @Override
    public CloudEventContextWriter withContextAttribute(String name, String value) throws CloudEventRWException {
        messageProperties.setHeader(CloudEventsHeaders.CE_PREFIX + name, value);
        return this;
    }

    @Override
    public MessageBuilderMessageWriter create(SpecVersion version) {
        messageProperties.setHeader(CloudEventsHeaders.SPEC_VERSION, version.toString());
        return this;
    }

}
