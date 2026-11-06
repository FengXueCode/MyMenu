package com.fengxue.menu.integration;

import com.fengxue.menu.ai.AiCipher;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.mapper.VideoAccountMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B 站扫码登录：生成二维码、轮询扫码状态、保存加密 Cookie。
 *
 * <p>二维码登录态只保存在内存，5 分钟内有效；成功后 Cookie 用 AES-GCM 加密入库，
 * 任何接口都不回传 Cookie。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BilibiliQrService {

    private static final long SESSION_TTL_SECONDS = 300;
    private static final String PLATFORM = "bilibili";

    private final BilibiliApi bilibiliApi;
    private final VideoAccountMapper accountMapper;
    private final AiCipher cipher;
    private final Map<String, QrSession> sessions = new ConcurrentHashMap<>();

    public QrStart start(Long userId) {
        cleanupExpired();
        BilibiliApi.HttpResult result = bilibiliApi.generateQr();
        BilibiliApi.assertCode(result.json());
        JsonNode data = result.json().path("data");
        String key = data.path("qrcode_key").asString("");
        String url = data.path("url").asString("");
        if (key.isBlank() || url.isBlank()) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "B站二维码生成失败，请稍后重试");
        }
        QrSession session = new QrSession(userId, System.currentTimeMillis());
        session.cookies.putAll(result.cookies());
        sessions.put(key, session);
        return new QrStart(key, "data:image/png;base64," + renderQrBase64(url));
    }

    public QrPoll poll(Long userId, String key) {
        QrSession session = sessions.get(key);
        if (session == null || !session.userId.equals(userId)) {
            return new QrPoll("EXPIRED", null, "二维码已失效，请重新生成");
        }
        if ((System.currentTimeMillis() - session.createdAt) / 1000 > SESSION_TTL_SECONDS) {
            sessions.remove(key);
            return new QrPoll("EXPIRED", null, "二维码已过期，请重新生成");
        }
        try {
            BilibiliApi.HttpResult result = bilibiliApi.pollQr(key,
                    BilibiliApi.cookieHeader(session.cookies));
            session.cookies.putAll(result.cookies());
            JsonNode data = result.json().path("data");
            int code = data.path("code").asInt(-1);
            if (code == 86101) {
                return new QrPoll("WAITING", null, "等待扫码");
            }
            if (code == 86090) {
                return new QrPoll("SCANNED", null, "已扫码，请在手机确认");
            }
            if (code == 86038) {
                sessions.remove(key);
                return new QrPoll("EXPIRED", null, "二维码已过期，请重新生成");
            }
            if (code != 0) {
                sessions.remove(key);
                return new QrPoll("FAILED", null,
                        data.path("message").asString("扫码确认失败，请重试"));
            }

            // 登录成功：用二维码会话 Cookie 请求 nav 校验并保存账号
            String cookieHeader = BilibiliApi.cookieHeader(session.cookies);
            JsonNode nav = bilibiliApi.nav(cookieHeader);
            JsonNode navData = nav.path("data");
            if (!navData.path("isLogin").asBoolean(false)) {
                sessions.remove(key);
                throw new BusinessException(ResultCode.IMPORT_FAILED, "B站登录状态校验失败，请重新扫码");
            }
            VideoAccount account = upsertAccount(userId, navData, session.cookies);
            sessions.remove(key);
            log.info("B站扫码登录成功: userId={}, mid={}", userId, account.getPlatformUid());
            return new QrPoll("CONFIRMED", toVo(account), "登录成功");
        } catch (BilibiliAuthException e) {
            sessions.remove(key);
            return new QrPoll("FAILED", null, e.getMessage());
        } catch (BusinessException e) {
            return new QrPoll("FAILED", null, e.getMessage());
        }
    }

    public VideoAccount getAccount(Long userId) {
        return accountMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<VideoAccount>lambdaQuery()
                        .eq(VideoAccount::getUserId, userId)
                        .eq(VideoAccount::getPlatform, PLATFORM)
                        .last("limit 1"));
    }

    public void disconnect(Long userId) {
        VideoAccount account = getAccount(userId);
        if (account != null) {
            accountMapper.deleteById(account.getId());
        }
    }

    public String cookieHeader(VideoAccount account) {
        if (account == null || account.getCookieCipher() == null) {
            return "";
        }
        try {
            CookieBundle bundle = JsonSupport.fromJson(
                    cipher.decrypt(account.getCookieCipher()), CookieBundle.class);
            return BilibiliApi.cookieHeader(bundle.cookies());
        } catch (Exception e) {
            throw new BilibiliAuthException("B站登录信息损坏，请重新扫码登录");
        }
    }

    public void markExpired(VideoAccount account) {
        if (account == null) {
            return;
        }
        account.setStatus("EXPIRED");
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    private VideoAccount upsertAccount(Long userId, JsonNode navData, Map<String, String> cookies) {
        VideoAccount account = getAccount(userId);
        if (account == null) {
            account = new VideoAccount();
            account.setUserId(userId);
            account.setPlatform(PLATFORM);
            account.setCreatedAt(LocalDateTime.now());
        }
        account.setPlatformUid(navData.path("mid").asString(""));
        account.setNickname(navData.path("uname").asString(""));
        account.setAvatar(navData.path("face").asString(""));
        account.setCookieCipher(cipher.encrypt(JsonSupport.toJson(cookies)));
        account.setStatus("ACTIVE");
        account.setLastLoginAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        if (account.getId() == null) {
            accountMapper.insert(account);
        } else {
            accountMapper.updateById(account);
        }
        return account;
    }

    private VideoAccountVo toVo(VideoAccount account) {
        return new VideoAccountVo(account.getId(), account.getPlatform(), account.getNickname(),
                account.getAvatar(), account.getStatus(), account.getLastLoginAt());
    }

    private String renderQrBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | java.io.IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "二维码生成失败");
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry ->
                (now - entry.getValue().createdAt) / 1000 > SESSION_TTL_SECONDS);
    }

    private static class QrSession {
        private final Long userId;
        private final long createdAt;
        private final Map<String, String> cookies = new LinkedHashMap<>();

        private QrSession(Long userId, long createdAt) {
            this.userId = userId;
            this.createdAt = createdAt;
        }
    }

    public record QrStart(String key, String qrImage) {
    }

    public record QrPoll(String status, VideoAccountVo account, String message) {
    }

    public record VideoAccountVo(Long id, String platform, String nickname, String avatar,
                                 String status, LocalDateTime lastLoginAt) {
    }

    public record CookieBundle(Map<String, String> cookies) {
    }
}
