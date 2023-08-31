package com.contentgrid.spring.querydsl.test.fixtures;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.BeanPath;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.StringPath;

public class QEmbeddedObject extends BeanPath<EmbeddedObject> {

    public QEmbeddedObject(String variable) {
        super(EmbeddedObject.class, variable);
    }

    public QEmbeddedObject(Path<?> parent, String property) {
        super(EmbeddedObject.class, parent, property);
    }

    public QEmbeddedObject(PathMetadata metadata) {
        super(EmbeddedObject.class, metadata);
    }

    public QEmbeddedObject(PathMetadata metadata, PathInits inits) {
        super(EmbeddedObject.class, metadata, inits);
    }

    public final StringPath stringValue = createString("stringValue");
}
