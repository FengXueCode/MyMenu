package com.fengxue.menu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 元信息配置。
 *
 * <p>访问地址由 application.yml 中的 springdoc 配置决定，默认：
 * Swagger UI 为 /swagger-ui.html，OpenAPI JSON 为 /v3/api-docs。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Menu · 灶台手账 API")
                        .description("菜谱、视频导入、AI 配置与代理、随机今日菜单接口文档")
                        .version("1.0.0"));
    }
}
