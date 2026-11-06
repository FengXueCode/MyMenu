package com.fengxue.menu.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AI Key 加解密：AES-256-GCM，随机 IV，密文 = Base64(iv + ciphertext + tag)。
 *
 * <p>主密钥首次启动生成到本地文件（默认 data/secret.key），不进数据库、不进代码仓库；
 * 可用环境变量 AI_KEY_MASTER（Base64 32 字节）覆盖。</p>
 */
@Component
public class AiCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    @org.springframework.beans.factory.annotation.Autowired
    public AiCipher(@Value("${app.ai.master-key-file:data/secret.key}") String masterKeyFile) {
        this.key = new SecretKeySpec(loadOrCreateKey(masterKeyFile), "AES");
    }

    /** 测试用构造器。 */
    AiCipher(byte[] keyBytes) {
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AI Key 加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AI Key 解密失败", e);
        }
    }

    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String key = apiKey.trim();
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }

    private static byte[] loadOrCreateKey(String masterKeyFile) {
        String envKey = System.getenv("AI_KEY_MASTER");
        if (envKey != null && !envKey.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(envKey.trim());
            if (decoded.length != 32) {
                throw new IllegalStateException("AI_KEY_MASTER 必须是 Base64 编码的 32 字节密钥");
            }
            return decoded;
        }
        try {
            Path path = Path.of(masterKeyFile).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                byte[] decoded = Base64.getDecoder().decode(Files.readString(path).trim());
                if (decoded.length == 32) {
                    return decoded;
                }
            }
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            Files.writeString(path, Base64.getEncoder().encodeToString(generated),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化 AI 主密钥文件: " + masterKeyFile, e);
        }
    }
}
