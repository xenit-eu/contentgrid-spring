package com.contentgrid.spring.content.encryption;

import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.TABLE_NAME;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.Field;
import org.jooq.Record;

@Entity
@Table(name = TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
public class ContentDataEncryptionKey {
    static final String TABLE_NAME = "_dek_storage";

    public static final org.jooq.Table<Record> DEK_STORAGE = table(name(TABLE_NAME));
    public static final Field<String> CONTENT_ID = field(name(TABLE_NAME, "content_id"), String.class);
    public static final Field<String> KEK_LABEL = field(name(TABLE_NAME, "kek_label"), String.class);
    public static final Field<byte[]> ENCRYPTED_DEK = field(name(TABLE_NAME, "encrypted_dek"), byte[].class);
    public static final Field<String> ALGORITHM = field(name(TABLE_NAME, "algorithm"), String.class);
    public static final Field<byte[]> INITIALIZATION_VECTOR = field(name(TABLE_NAME, "iv"), byte[].class);

    @Id
    @Column(name = "content_id")
    private String contentId;

    @Id
    @Column(name = "kek_label")
    private String kekLabel;

    @Column(name = "encrypted_dek")
    private byte[] encryptedDek;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "iv")
    private byte[] initializationVector;

}
