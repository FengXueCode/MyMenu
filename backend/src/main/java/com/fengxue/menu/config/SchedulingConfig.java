package com.fengxue.menu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 定时任务与视频收藏夹扫描线程池。
 *
 * <p>扫描会调用外部 B 站接口与 AI，单次可能持续数分钟，因此在独立线程池执行，
 * 页面通过轮询 source.lastScanStatus 查看进度。</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean(name = "integrationScanExecutor")
    public ThreadPoolTaskExecutor integrationScanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integration-scan-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
