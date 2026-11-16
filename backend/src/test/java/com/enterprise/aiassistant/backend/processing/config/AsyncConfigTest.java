package com.enterprise.aiassistant.backend.processing.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the document-processing executor is configured as expected.
 */
class AsyncConfigTest {

    @Test
    void shouldConfigureDocumentProcessingExecutor() {

        Executor executor = new AsyncConfig().documentProcessingExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;

        assertThat(pool.getCorePoolSize()).isEqualTo(3);
        assertThat(pool.getMaxPoolSize()).isEqualTo(5);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("document-processing-");
        assertThat(pool.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(100);

        pool.shutdown();
    }
}
