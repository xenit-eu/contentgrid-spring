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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractEntityAuditEvent extends AuditEvent {
    private final Class<?> domainType;

    protected AbstractEntityAuditEvent(AbstractEntityAuditEventBuilder<?, ?> b) {
        super(b);
        this.domainType = b.domainType;
    }
}
