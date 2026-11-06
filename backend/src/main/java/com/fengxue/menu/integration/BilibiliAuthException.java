package com.fengxue.menu.integration;

/**
 * B 站 Cookie 失效（接口返回 -101 / 未登录）。
 */
public class BilibiliAuthException extends RuntimeException {

    public BilibiliAuthException(String message) {
        super(message);
    }
}
