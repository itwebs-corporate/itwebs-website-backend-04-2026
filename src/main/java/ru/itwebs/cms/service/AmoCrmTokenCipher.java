package ru.itwebs.cms.service;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AmoCrmTokenCipher {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final AmoCrmProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AmoCrmTokenCipher(AmoCrmProperties properties) { this.properties = properties; }

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt amoCRM token", exception);
        }
    }

    public String decrypt(String value) {
        try {
            byte[] packed = Base64.getDecoder().decode(value);
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt amoCRM token; check AMOCRM_TOKEN_ENCRYPTION_KEY", exception);
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] bytes = Base64.getDecoder().decode(properties.getTokenEncryptionKey());
            if (bytes.length != 32) throw new IllegalArgumentException("AES-256 key must be 32 bytes");
            return new SecretKeySpec(bytes, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("AMOCRM_TOKEN_ENCRYPTION_KEY must be base64-encoded 32-byte key", exception);
        }
    }
}
