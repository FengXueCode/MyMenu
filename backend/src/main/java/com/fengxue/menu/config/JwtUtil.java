package com.fengxue.menu.config;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 JWT（HS256）实现。
 *
 * <p>密钥在首次启动时生成到本地文件（默认 {@code data/jwt.secret}），不会写入代码仓库；
 * 也可通过环境变量 {@code JWT_SECRET_FILE} 指定路径。选择自实现而非引入第三方库，
 * 是为了避免 JWT 库与 Spring Boot 4 / Jackson 3 的版本耦合。</p>
 */
@Component
public class JwtUtil {

    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long expireSeconds;

    public JwtUtil(@Value("${app.jwt.secret-file:data/jwt.secret}") String secretFile,
                   @Value("${app.jwt.expire-days:7}") long expireDays) {
        this.secret = loadOrCreateSecret(secretFile);
        this.expireSeconds = Math.max(1, expireDays) * 24 * 3600;
    }

    public String createToken(long userId) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("iat", now);
        payload.put("exp", now + expireSeconds);

        String header = B64_ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String body = B64_ENCODER.encodeToString(JsonSupport.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + body;
        return signingInput + "." + B64_ENCODER.encodeToString(hmac(signingInput));
    }

    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmac(signingInput);
            byte[] actual = B64_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            JsonNode payload = JsonSupport.readTree(
                    new String(B64_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
            long exp = payload.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            return Long.parseLong(payload.path("sub").asString("0"));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    private byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private static byte[] loadOrCreateSecret(String secretFile) {
        try {
            Path path = Path.of(secretFile).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                byte[] bytes = Base64.getDecoder().decode(Files.readString(path).trim());
                if (bytes.length >= 32) {
                    return bytes;
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
            throw new IllegalStateException("无法初始化 JWT 密钥文件: " + secretFile, e);
        }
    }
}
