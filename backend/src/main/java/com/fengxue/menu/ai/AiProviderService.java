package com.fengxue.menu.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.mapper.AiProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService {

    private static final Set<String> PROVIDER_TYPES =
            Set.of("openai", "deepseek", "qwen", "moonshot", "siliconflow", "ollama", "custom");
    private static final Pattern BASE_URL = Pattern.compile("^https?://[^\\s]+$");

    private final AiProviderMapper providerMapper;
    private final AiCipher cipher;
    private final AiClient aiClient;

    public List<AiDtos.ProviderVO> list(Long userId) {
        return providerMapper.selectList(Wrappers.<AiProvider>lambdaQuery()
                        .eq(AiProvider::getUserId, userId)
                        .orderByDesc(AiProvider::getIsDefault)
                        .orderByAsc(AiProvider::getId))
                .stream().map(this::toVo).toList();
    }

    @Transactional
    public AiDtos.ProviderVO create(Long userId, AiDtos.ProviderInput input) {
        validate(input);
        boolean first = providerMapper.selectCount(
                Wrappers.<AiProvider>lambdaQuery().eq(AiProvider::getUserId, userId)) == 0;
        boolean makeDefault = Boolean.TRUE.equals(input.isDefault()) || first;
        if (makeDefault) {
            clearDefault(userId);
        }
        AiProvider provider = new AiProvider();
        provider.setUserId(userId);
        provider.setName(input.name().trim());
        provider.setProviderType(input.providerType());
        provider.setBaseUrl(trimTrailingSlash(input.baseUrl()));
        provider.setChatModel(input.chatModel().trim());
        provider.setImageModel(blankToNull(input.imageModel()));
        if (input.apiKey() != null && !input.apiKey().isBlank()) {
            String key = input.apiKey().trim();
            provider.setApiKeyCipher(cipher.encrypt(key));
            provider.setApiKeyHint(AiCipher.mask(key));
        }
        provider.setEnabled(Boolean.FALSE.equals(input.enabled()) ? 0 : 1);
        provider.setIsDefault(makeDefault ? 1 : 0);
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        providerMapper.insert(provider);
        return toVo(provider);
    }

    @Transactional
    public AiDtos.ProviderVO update(Long userId, Long id, AiDtos.ProviderInput input) {
        AiProvider provider = requireOwned(userId, id);
        validate(input);
        if (Boolean.TRUE.equals(input.isDefault()) && provider.getIsDefault() == 0) {
            clearDefault(userId);
            provider.setIsDefault(1);
        }
        provider.setName(input.name().trim());
        provider.setProviderType(input.providerType());
        provider.setBaseUrl(trimTrailingSlash(input.baseUrl()));
        provider.setChatModel(input.chatModel().trim());
        provider.setImageModel(blankToNull(input.imageModel()));
        if (input.apiKey() != null && !input.apiKey().isBlank()
                && !input.apiKey().contains("****")) {
            String key = input.apiKey().trim();
            provider.setApiKeyCipher(cipher.encrypt(key));
            provider.setApiKeyHint(AiCipher.mask(key));
        }
        if (input.enabled() != null) {
            provider.setEnabled(input.enabled() ? 1 : 0);
        }
        provider.setUpdatedAt(LocalDateTime.now());
        providerMapper.updateById(provider);
        return toVo(provider);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        AiProvider provider = requireOwned(userId, id);
        providerMapper.deleteById(id);
        if (provider.getIsDefault() == 1) {
            AiProvider next = providerMapper.selectOne(Wrappers.<AiProvider>lambdaQuery()
                    .eq(AiProvider::getUserId, userId)
                    .orderByAsc(AiProvider::getId)
                    .last("limit 1"));
            if (next != null) {
                next.setIsDefault(1);
                providerMapper.updateById(next);
            }
        }
    }

    public AiDtos.TestResult test(Long userId, Long id) {
        AiProvider provider = requireOwned(userId, id);
        String apiKey = cipher.decrypt(provider.getApiKeyCipher());
        long begin = System.currentTimeMillis();
        try {
            aiClient.test(provider, apiKey);
            long latency = System.currentTimeMillis() - begin;
            updateTestStatus(provider, 1);
            return new AiDtos.TestResult(true, "连接成功，模型：" + provider.getChatModel(), latency);
        } catch (BusinessException e) {
            updateTestStatus(provider, 0);
            return new AiDtos.TestResult(false, e.getMessage(), System.currentTimeMillis() - begin);
        }
    }

    public AiProvider requireDefault(Long userId) {
        AiProvider provider = providerMapper.selectOne(Wrappers.<AiProvider>lambdaQuery()
                .eq(AiProvider::getUserId, userId)
                .eq(AiProvider::getEnabled, 1)
                .orderByDesc(AiProvider::getIsDefault)
                .orderByAsc(AiProvider::getId)
                .last("limit 1"));
        if (provider == null) {
            throw new BusinessException(ResultCode.AI_NOT_CONFIGURED);
        }
        return provider;
    }

    public String apiKeyOf(AiProvider provider) {
        return cipher.decrypt(provider.getApiKeyCipher());
    }

    public AiProvider requireOwned(Long userId, Long id) {
        AiProvider provider = providerMapper.selectById(id);
        if (provider == null || !provider.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    private void clearDefault(Long userId) {
        providerMapper.update(null, Wrappers.<AiProvider>lambdaUpdate()
                .eq(AiProvider::getUserId, userId)
                .set(AiProvider::getIsDefault, 0));
    }

    private void updateTestStatus(AiProvider provider, int ok) {
        provider.setLastTestOk(ok);
        provider.setUpdatedAt(LocalDateTime.now());
        providerMapper.updateById(provider);
        log.info("AI 配置连通性测试: providerId={}, model={}, ok={}",
                provider.getId(), provider.getChatModel(), ok == 1);
    }

    private AiDtos.ProviderVO toVo(AiProvider provider) {
        return new AiDtos.ProviderVO(
                provider.getId(), provider.getName(), provider.getProviderType(),
                provider.getBaseUrl(), provider.getChatModel(), provider.getImageModel(),
                provider.getApiKeyCipher() != null && !provider.getApiKeyCipher().isBlank(),
                provider.getApiKeyHint(),
                provider.getEnabled() != null && provider.getEnabled() == 1,
                provider.getIsDefault() != null && provider.getIsDefault() == 1,
                provider.getLastTestOk() == null ? null : provider.getLastTestOk() == 1);
    }

    private void validate(AiDtos.ProviderInput input) {
        if (input.name() == null || input.name().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配置名称不能为空");
        }
        if (input.providerType() == null || !PROVIDER_TYPES.contains(input.providerType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的服务商类型");
        }
        if (input.baseUrl() == null || !BASE_URL.matcher(input.baseUrl().trim()).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Base URL 需以 http:// 或 https:// 开头");
        }
        try {
            URI.create(input.baseUrl().trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Base URL 格式不正确");
        }
        if (input.chatModel() == null || input.chatModel().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "对话模型不能为空");
        }
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
