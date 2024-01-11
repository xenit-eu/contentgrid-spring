package com.contentgrid.spring.audit.event;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
@ToString
public abstract class AuditEvent {

    public abstract static class AuditEventBuilder<C extends AuditEvent, B extends AuditEvent.AuditEventBuilder<C, B>> {

    }



}
