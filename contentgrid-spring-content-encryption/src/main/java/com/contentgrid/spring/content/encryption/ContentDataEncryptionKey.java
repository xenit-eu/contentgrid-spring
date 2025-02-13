package com.contentgrid.spring.content.encryption;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "encryption", name = "dek_storage")
@Getter
@Setter
@NoArgsConstructor
public class ContentDataEncryptionKey {

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
