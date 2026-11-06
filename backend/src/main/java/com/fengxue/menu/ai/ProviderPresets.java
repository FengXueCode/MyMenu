package com.fengxue.menu.ai;

import java.util.List;

/**
 * 常见 OpenAI 兼容服务商预设，前端用于一键填充。
 */
public final class ProviderPresets {

    private ProviderPresets() {
    }

    public static List<AiDtos.Preset> all() {
        return List.of(
                new AiDtos.Preset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "gpt-image-1"),
                new AiDtos.Preset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", ""),
                new AiDtos.Preset("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "qwen-plus", ""),
                new AiDtos.Preset("moonshot", "Kimi / Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k", ""),
                new AiDtos.Preset("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1",
                        "Qwen/Qwen2.5-7B-Instruct", "Kwai-Kolors/Kolors"),
                new AiDtos.Preset("opencode", "OpenCode Zen", "https://opencode.ai/inference/openai/v1",
                        "deepseek-v4.1-flash", ""),
                new AiDtos.Preset("ollama", "Ollama（本地）", "http://localhost:11434/v1", "llama3.1", ""),
                new AiDtos.Preset("custom", "自定义 OpenAI 兼容", "", "", ""));
    }
}
