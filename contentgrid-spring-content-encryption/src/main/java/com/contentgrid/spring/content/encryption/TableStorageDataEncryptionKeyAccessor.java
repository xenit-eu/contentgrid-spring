package com.contentgrid.spring.content.encryption;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

@RequiredArgsConstructor
public class TableStorageDataEncryptionKeyAccessor<S> implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {
    private final DSLContext dslContext;
    private final String wrappingKeyLabel;

    @Override
    public Collection<UnencryptedSymmetricDataEncryptionKey> findKeys(S entity, ContentProperty contentProperty) {
        var contentId = (String)contentProperty.getContentId(entity);
        if (contentId == null) {
            return null;
        }
        var dekStorage = table(name("encryption", "dek_storage"));
        var result = dslContext.select(dekStorage.asterisk()).from(dekStorage)
                .where(field("content_id").eq(contentId).and(field("kek_label").eq(wrappingKeyLabel)))
                .fetchInto(ContentDataEncryptionKey.class);
        if (result.isEmpty()) {
            return null; // Not encrypted
        }
        return result.stream().map(record -> new UnencryptedSymmetricDataEncryptionKey(
                record.getAlgorithm(),
                record.getEncryptedDek(),
                record.getInitializationVector()
        )).toList();
    }

    @Override
    public S setKeys(S entity, ContentProperty contentProperty,
            Collection<UnencryptedSymmetricDataEncryptionKey> dataEncryptionKeys) {
        var dekStorage = table(name("encryption", "dek_storage"));
        var contentId = (String)contentProperty.getContentId(entity);
        if (contentId == null) {
            throw new IllegalArgumentException("No value for contentId present.");
        }
        if (dataEncryptionKeys.size() > 1) {
            // Multiple versions of the same data key, but all unencrypted!
            throw new IllegalArgumentException("Multiple unencrypted versions of the same data key provided.");
        }
        dslContext.delete(dekStorage)
                .where(field("content_id").eq(contentId).and(field("kek_label").eq(wrappingKeyLabel)))
                .execute();
        for (var dataEncryptionKey : dataEncryptionKeys) {
            if (dataEncryptionKey == null) {
                continue;
            }

            dslContext.insertInto(dekStorage)
                    .set(field("content_id"), contentId)
                    .set(field("algorithm"), dataEncryptionKey.getAlgorithm())
                    .set(field("encrypted_dek"), dataEncryptionKey.getKeyData())
                    .set(field("iv"), dataEncryptionKey.getInitializationVector())
                    .set(field("kek_label"), wrappingKeyLabel)
                    .execute();
        }

        return entity;
    }
}
