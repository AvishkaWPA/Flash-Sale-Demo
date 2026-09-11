package com.avishka.FlashSale.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl:false}")
    private boolean useSsl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String protocol = useSsl ? "rediss://" : "redis://";

        var singleServerConfig = config.useSingleServer()
              .setAddress(protocol + redisHost + ":" + redisPort)
              .setConnectionMinimumIdleSize(5)
              .setConnectionPoolSize(32)
              .setConnectTimeout(10000)
              .setTimeout(3000);

        if (redisPassword != null && !redisPassword.isBlank()) {
            singleServerConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
