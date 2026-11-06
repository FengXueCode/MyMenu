package com.fengxue.menu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模板工程启动类。
 *
 * <p>启动后默认使用 dev 配置，接口文档地址：
 * <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>。</p>
 */
@SpringBootApplication
public class MenuApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenuApplication.class, args);
    }
}
