package com.fengxue.menu.config;

import com.fengxue.menu.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @TempDir
    Path tempDir;

    private JwtUtil newJwtUtil() {
        return new JwtUtil(tempDir.resolve("jwt.secret").toString(), 7);
    }

    @Test
    void createAndParseToken() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.createToken(42L);

        assertEquals(42L, jwtUtil.parseUserId(token));
        assertEquals(3, token.split("\\.").length);
        assertTrue(Files.exists(tempDir.resolve("jwt.secret")));
    }

    @Test
    void tokenIsStableAcrossInstancesUsingSameSecretFile() {
        JwtUtil first = newJwtUtil();
        String token = first.createToken(7L);
        JwtUtil second = newJwtUtil();

        assertEquals(7L, second.parseUserId(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.createToken(1L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(BusinessException.class, () -> jwtUtil.parseUserId(tampered));
    }

    @Test
    void malformedTokenIsRejected() {
        JwtUtil jwtUtil = newJwtUtil();
        assertThrows(BusinessException.class, () -> jwtUtil.parseUserId("not-a-jwt"));
    }

    @Test
    void differentUsersProduceDifferentTokens() {
        JwtUtil jwtUtil = newJwtUtil();
        assertNotEquals(jwtUtil.createToken(1L), jwtUtil.createToken(2L));
    }
}
