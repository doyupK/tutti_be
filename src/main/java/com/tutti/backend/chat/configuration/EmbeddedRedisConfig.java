package com.tutti.backend.chat.configuration;


// import 생략...

//import lombok.Value;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

// import 생략...

/**
 * 로컬 환경일경우 내장 레디스가 실행됩니다.
 */
@Profile("local")
@Configuration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void redisServer() {

        redisServer = RedisServer.builder()
                .port(6379)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}