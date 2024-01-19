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

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventContext;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.impl.GenericStructuredMessageReader;
import io.cloudevents.core.message.impl.MessageUtils;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * A {@link MessageConverter} that can translate to and from a {@link Message Message} and a {@link CloudEvent}. The
 * {@link CloudEventContext} is canonicalized, with key names given a {@code cloudEvents} prefix in the
 * {@link MessageProperties}.
 * <p>
 * Modified from <a
 * href="https://github.com/cloudevents/sdk-java/tree/fb11b94f2b534858f698fce3b122fa6625f2d184/spring/src/main/java/io/cloudevents/spring/messaging">CloudEvents
 * Spring</a> to use the AMQP MessageConverter instead of spring integration messaging
 *
 * @author Dave Syer
 * @author Lars Vierbergen
 */
@RequiredArgsConstructor
public class CloudEventMessageConverter implements MessageConverter {

    private final MessageConverter delegate;

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        if (message.getMessageProperties().getHeader(CloudEventsHeaders.SPEC_VERSION) != null) {
            return createMessageReader(message).toEvent();
        }
        return delegate.fromMessage(message);
    }

    @Override
    @SneakyThrows(URISyntaxException.class)
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (object instanceof CloudEvent event) {
            var sourceUri = event.getSource();
            var messageIdUri = new URI(
                    sourceUri.getScheme(),
                    sourceUri.getSchemeSpecificPart(),
                    sourceUri.getFragment() != null ? sourceUri.getFragment() + "#" + event.getId() : event.getId()
            );
            messageProperties.setMessageId(messageIdUri.toASCIIString());
            return CloudEventUtils.toReader(event)
                    .read(new MessageBuilderMessageWriter(messageProperties));
        }
        return delegate.toMessage(object, messageProperties);
    }

    private MessageReader createMessageReader(Message message) {
        return MessageUtils.parseStructuredOrBinaryMessage( //
                () -> contentType(message.getMessageProperties()), //
                format -> structuredMessageReader(message, format), //
                () -> version(message.getMessageProperties()), //
                version -> binaryMessageReader(message, version) //
        );
    }

    private String version(MessageProperties messageProperties) {
        if (messageProperties.getHeader(CloudEventsHeaders.SPEC_VERSION) != null) {
            return messageProperties.getHeader(CloudEventsHeaders.SPEC_VERSION).toString();
        }
        return null;
    }

    private MessageReader binaryMessageReader(Message message, SpecVersion version) {
        return new MessageBinaryMessageReader(version, message.getMessageProperties(), message.getBody());
    }

    private MessageReader structuredMessageReader(Message message, EventFormat format) {
        return new GenericStructuredMessageReader(format, message.getBody());
    }

    private String contentType(MessageProperties messageProperties) {
        if (messageProperties.getContentType() != null) {
            return messageProperties.getContentType();
        }
        if (messageProperties.getHeader(CloudEventsHeaders.CONTENT_TYPE) != null) {
            return messageProperties.getHeader(CloudEventsHeaders.CONTENT_TYPE).toString();
        }
        return null;
    }

}
