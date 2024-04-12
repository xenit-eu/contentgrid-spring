package com.contentgrid.spring.boot.autoconfigure.actuator;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Value
@RequiredArgsConstructor
public class ContentGridExposedActuatorEndpoint {

    Class<?> endpoint;

    public RequestMatcher toRequestMatcher() {
        return EndpointRequest.to(endpoint);
    }

}
