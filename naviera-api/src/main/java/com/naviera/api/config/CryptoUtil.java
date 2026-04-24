package com.naviera.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra AES-GCM para dados sensiveis at-rest (ex: totp_secret).
 *
 * Chave: `naviera.crypto.totp-key` em application.properties — 32 bytes em base64.
 * Formato cifrado: `ENC:<base64(iv || ciphertext || tag)>`.
 *
 * Legado em plain text continua aceito pela `decrypt()` (quando nao comeca com ENC:).
 */
@Component
public class CryptoUtil {

    private static final String PREFIX = "ENC:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RND = new SecureRandom();

    private final SecretKey key;

    public CryptoUtil(@Value("${naviera.crypto.totp-key:}") String keyB64) {
        if (keyB64 == null || keyB64.isBlank()) {
            this.key = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(keyB64);
        if (raw.length != 32) throw new IllegalStateException("naviera.crypto.totp-key precisa ter 32 bytes (base64)");
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        if (key == null) return plain; // dev sem chave: armazena em claro
        try {
            byte[] iv = new byte[IV_BYTES];
            RND.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar: " + e.getMessage(), e);
        }
    }

    public String decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX)) return value;
        if (key == null) throw new IllegalStateException("Valor cifrado mas naviera.crypto.totp-key ausente");
        try {
            byte[] in = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[in.length - IV_BYTES];
            System.arraycopy(in, 0, iv, 0, IV_BYTES);
            System.arraycopy(in, IV_BYTES, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar: " + e.getMessage(), e);
        }
    }
}
