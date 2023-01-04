package com.project.trysketch.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // 내가 직접 호스트하고 port 를 지정해서 사용하려면 아래의 것을 사용
    // 자바의 Redis Client 중 더 성능이 좋다는 Lettuce 를 사용
/*    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }
    */

    // Redis 와 연결
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 일반적인 Key:Value 형태의 경우 시리얼라이저
        // Serializer(직렬화) Spring 과 Redis 간 데이터 직, 역직렬화시 사용하는 방식이 JDK 직렬화 방식이다
        // redis-cli 를 통해 직접 데이터를 보려고 할 때 알아볼 수 있는 형태로 출력하기 위해서
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        // Hash 를 사용할 경우 시리얼라이저
        //redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        //모든 경우
        //redisTemplate.setDefaultSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
