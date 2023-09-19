package com.contentgrid.spring.querydsl.test.fixtures;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.BeanPath;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.ListPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathInits;
import com.querydsl.core.types.dsl.SetPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.TimePath;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Getter
@Accessors(fluent = true)
public class QTestObject extends EntityPathBase<TestObject> {
    private static final PathInits INITS = PathInits.DIRECT2;

    public QTestObject(String variable) {
        this(forVariable(variable), INITS);
    }

    public QTestObject(Path<? extends TestObject> path) {
        this(path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTestObject(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTestObject(PathMetadata metadata, PathInits inits) {
        super(TestObject.class, metadata, inits);
        this.embeddedObject = new QEmbeddedObject(forProperty("embeddedObject"), inits.get("embeddedObject"));
    }

    public final StringPath stringValue = createString("stringValue");
    public final TimePath<Instant> timeValue = createTime("timeValue", Instant.class);
    public final NumberPath<Integer> intValue = createNumber("intValue", Integer.class);
    public final BooleanPath booleanValue = createBoolean("booleanValue");
    public final ComparablePath<UUID> uuidValue = createComparable("uuidValue", UUID.class);

    public final ListPath<String, StringPath> stringItems = createList("stringItems", String.class, StringPath.class, INITS);

    public final QEmbeddedObject embeddedObject;

    public final SetPath<EmbeddedObject, QEmbeddedObject> embeddedItems = createSet("embeddedItems", EmbeddedObject.class, QEmbeddedObject.class, INITS);

}
