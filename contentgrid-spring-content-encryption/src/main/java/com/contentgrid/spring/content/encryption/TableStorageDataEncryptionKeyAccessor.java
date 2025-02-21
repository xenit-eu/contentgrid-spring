package com.contentgrid.spring.content.encryption;

import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.ALGORITHM;
import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.CONTENT_ID;
import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.DEK_STORAGE;
import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.ENCRYPTED_DEK;
import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.INITIALIZATION_VECTOR;
import static com.contentgrid.spring.content.encryption.ContentDataEncryptionKey.KEK_LABEL;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class TableStorageDataEncryptionKeyAccessor<S> implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {
    private final DSLContext dslContext;
    private static final String WRAPPING_KEY_LABEL = "";

    @Override
    public Collection<UnencryptedSymmetricDataEncryptionKey> findKeys(S entity, ContentProperty contentProperty) {
        var contentId = (String)contentProperty.getContentId(entity);
        if (contentId == null) {
            return null;
        }
        var result = dslContext.select(DEK_STORAGE.asterisk()).from(DEK_STORAGE)
                .where(CONTENT_ID.eq(contentId).and(KEK_LABEL.eq(WRAPPING_KEY_LABEL)))
                .fetchInto(ContentDataEncryptionKey.class);
        if (result.isEmpty()) {
            return null; // Not encrypted
        }
        return result.stream().map(value -> new UnencryptedSymmetricDataEncryptionKey(
                value.getAlgorithm(),
                value.getEncryptedDek(),
                value.getInitializationVector()
        )).toList();
    }

    @Override
    @Transactional
    public S setKeys(S entity, ContentProperty contentProperty,
            Collection<UnencryptedSymmetricDataEncryptionKey> dataEncryptionKeys) {
        var contentId = (String)contentProperty.getContentId(entity);
        if (contentId == null) {
            throw new IllegalArgumentException("No value for contentId present.");
        }
        if (dataEncryptionKeys.size() > 1) {
            // Multiple versions of the same data key, but all unencrypted!
            throw new IllegalArgumentException("Multiple unencrypted versions of the same data key provided.");
        }

        dataEncryptionKeys.stream().findFirst()
                .ifPresentOrElse(dataEncryptionKey -> {
                    dslContext.insertInto(DEK_STORAGE)
                            .set(CONTENT_ID, contentId)
                            .set(KEK_LABEL, WRAPPING_KEY_LABEL)
                            .set(ALGORITHM, dataEncryptionKey.getAlgorithm())
                            .set(ENCRYPTED_DEK, dataEncryptionKey.getKeyData())
                            .set(INITIALIZATION_VECTOR, dataEncryptionKey.getInitializationVector())
                            .onConflict(CONTENT_ID, KEK_LABEL)
                            .doUpdate()
                            .set(ALGORITHM, dataEncryptionKey.getAlgorithm())
                            .set(ENCRYPTED_DEK, dataEncryptionKey.getKeyData())
                            .set(INITIALIZATION_VECTOR, dataEncryptionKey.getInitializationVector())
                            .execute();
                }, () -> {
                    dslContext.delete(DEK_STORAGE)
                            .where(CONTENT_ID.eq(contentId).and(KEK_LABEL.eq(WRAPPING_KEY_LABEL)))
                            .execute();
                });

        return entity;
    }
}
