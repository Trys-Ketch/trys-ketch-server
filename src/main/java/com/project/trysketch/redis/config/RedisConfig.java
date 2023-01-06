package com.project.trysketch.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// 1. 기능   : Redis Config
// 2. 작성자 : 서혁수
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

    /*
        Lettuce: Multi-Thread 에서 Thread-Safe 한 Redis 클라이언트로 netty 에 의해 관리된다.
                 Sentinel, Cluster, Redis data model 같은 고급 기능들을 지원하며
                 비동기 방식으로 요청하기에 TPS/CPU/Connection 개수와 응답속도 등 전 분야에서 Jedis 보다 뛰어나다.
                 스프링 부트의 기본 의존성은 현재 Lettuce 로 되어있다.

        Jedis  : Multi-Thread 에서 Thread-unsafe 하며 Connection pool 을 이용해 멀티쓰레드 환경을 구성한다.
                 Jedis 인스턴스와 연결할 때마다 Connection pool 을 불러오고 스레드 갯수가
                 늘어난다면 시간이 상당히 소요될 수 있다.
     */

    // Redis 와 연결
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    /*
        RedisTemplate: Redis data access code 를 간소화 하기 위해 제공되는 클래스이다.
                       주어진 객체들을 자동으로 직렬화/역직렬화 하며 binary 데이터를 Redis 에 저장한다.
                       기본설정은 JdkSerializationRedisSerializer 이다.

        StringRedisSerializer: binary 데이터로 저장되기 때문에 이를 String 으로 변환시켜주며(반대로도 가능) UTF-8 인코딩 방식을 사용한다.

        GenericJackson2JsonRedisSerializer: 객체를 json 타입으로 직렬화/역직렬화를 수행한다.
     */

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 일반적인 Key:Value 형태의 경우 시리얼라이저
        // Serializer(직렬화) Spring 과 Redis 간 데이터 직, 역직렬화시 사용하는 방식이 JDK 직렬화 방식이다
        // redis-cli 를 통해 직접 데이터를 보려고 할 때 알아볼 수 있는 형태로 출력하기 위해서
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Hash 를 사용할 경우 시리얼라이저
        // redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        // 모든 경우
        // redisTemplate.setDefaultSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

}
