package com.contentgrid.spring.audit.event;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
@ToString
public abstract class AbstractAuditEvent {

    String requestMethod;
    String requestUri;

    int responseStatus;
    String responseLocation;

}
