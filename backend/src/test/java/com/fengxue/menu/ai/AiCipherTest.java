package com.fengxue.menu.ai;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCipherTest {

    private final AiCipher cipher = new AiCipher(randomKey());

    @Test
    void encryptThenDecrypt() {
        String plain = "sk-test-1234567890abcdef";
        String encrypted = cipher.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, cipher.decrypt(encrypted));
    }

    @Test
    void samePlainTextProducesDifferentCipherText() {
        String first = cipher.encrypt("sk-same-key");
        String second = cipher.encrypt("sk-same-key");

        assertNotEquals(first, second);
        assertEquals("sk-same-key", cipher.decrypt(first));
        assertEquals("sk-same-key", cipher.decrypt(second));
    }

    @Test
    void tamperedCipherTextIsRejected() {
        String encrypted = cipher.encrypt("sk-secret");
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void blankValuesAreSafe() {
        assertNull(cipher.encrypt(""));
        assertNull(cipher.decrypt(null));
    }

    @Test
    void maskHidesMostOfKey() {
        assertEquals("sk-****cdef", AiCipher.mask("sk-1234567890abcdef"));
        assertEquals("****", AiCipher.mask("short"));
        assertNull(AiCipher.mask(" "));
    }

    private byte[] randomKey() {
        byte[] key = new byte[32];
        new Random(7).nextBytes(key);
        return key;
    }
}
