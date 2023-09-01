package com.contentgrid.spring.querydsl.test.fixtures;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Value;

@Value
public class TestObject {

    String stringValue;
    Instant timeValue;
    Boolean booleanValue;
    int intValue;
    UUID uuidValue;

    List<String> stringItems;

    EmbeddedObject embeddedObject;

    Set<EmbeddedObject> embeddedItems;
}
