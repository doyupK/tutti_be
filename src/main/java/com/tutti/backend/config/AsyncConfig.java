

package com.tutti.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig extends AsyncConfigurerSupport {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        기본 실행 대기하는 스레드 개수
        executor.setCorePoolSize(5);
//        동시에 실행되는 최대 스레드 개수
        executor.setMaxPoolSize(50);
//        MaxPoolSize가 넘어가는 스레드 요청 시 Queue에 저장하는데, 최대 Queue에 저장 가능한 개수
        executor.setQueueCapacity(100);
//        생선되는 스레드의 접두사
        executor.setThreadNamePrefix("tutti_ASYC_THREAD");
        executor.initialize();
        return executor;
    }
}
