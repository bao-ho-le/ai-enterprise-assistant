package com.enterprise.aiassistant.backend.processing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("documentProcessingExecutor")
    public Executor documentProcessingExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Số lượng thread luôn được duy trì trong pool.
        // Tối đa 3 tác vụ sẽ được xử lý song song ngay cả khi tải thấp.
        executor.setCorePoolSize(3);

        // Số lượng thread tối đa có thể tạo khi tất cả thread core đều bận
        // và hàng đợi (queue) đã đầy.
        executor.setMaxPoolSize(5);

        // Số lượng tác vụ tối đa được phép chờ trong hàng đợi
        // khi tất cả thread hiện tại đều đang bận.
        // Nếu queue đầy và đã đạt maxPoolSize, tác vụ mới sẽ bị từ chối.
        executor.setQueueCapacity(100);

        // Tiền tố tên của các thread, giúp dễ nhận biết khi xem log hoặc debug.
        executor.setThreadNamePrefix("document-processing-");

        executor.initialize();

        return executor;
    }
}