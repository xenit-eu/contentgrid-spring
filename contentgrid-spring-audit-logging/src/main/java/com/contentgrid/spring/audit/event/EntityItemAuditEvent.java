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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityItemAuditEvent extends AbstractEntityItemAuditEvent {

    Operation operation;

    public enum Operation {
        READ,
        CREATE,
        UPDATE,
        DELETE
    }
}
