package com.gomokumatching.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous method execution.
 *
 * Enables @Async annotation processing with custom thread pool.
 *
 * Used by:
 * - GameEventProducer for Kafka event publishing
 * - Any other @Async methods in the application
 *
 * Thread Pool Settings:
 * - Core pool size: 2 threads (minimum always running)
 * - Max pool size: 5 threads (maximum concurrent threads)
 * - Queue capacity: 100 tasks (buffered before rejection)
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Configure task executor for @Async methods.
     *
     * Thread pool prevents Kafka publishing from blocking game operations.
     *
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // core threads always alive
        executor.setCorePoolSize(2);

        // max threads under load
        executor.setMaxPoolSize(5);

        // queue capacity before rejecting tasks
        executor.setQueueCapacity(100);

        // thread naming for easier debugging in logs
        executor.setThreadNamePrefix("async-kafka-");

        // wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized async task executor: corePoolSize=2, maxPoolSize=5, queueCapacity=100");

        return executor;
    }

    /**
     * Handle uncaught exceptions in @Async methods.
     *
     * Logs errors without crashing the application.
     * Critical for Kafka publishing - errors shouldn't break game flow.
     *
     * @return AsyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("❌ Uncaught async exception in method {}: {}",
                    method.getName(),
                    ex.getMessage(),
                    ex);
        };
    }
}
