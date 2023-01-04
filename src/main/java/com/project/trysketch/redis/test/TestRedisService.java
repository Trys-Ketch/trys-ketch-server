package com.project.trysketch.redis.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestRedisService {

    /**
     * RedisTemplate 의 opsFor 메소드 종류
     * 메소드명	        반환 오퍼레이션        관련 Redis 자료구조
     * opsForValue()    ValueOperations	    String
     * opsForList()	    ListOperations	    List
     * opsForSet()	    ListOperations	    List
     * opsForList()	    SetOperations	    Set
     * opsForZSet()	    ZSetOperations	    Sorted Set
     * opsForHash()	    HashOperations	    Hash
     */

    // get/set 을 위한 객체
    // 자바 객체를 redis 에 저장하려고 할 때 사용한다.
    private final RedisTemplate<String, Object> redisTemplate;
    // 문자열에 특화된 template 를 제공, 대부분의 redis Key/Value 는 문자열 위주
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 전달받은 메시지 객체를 각각 redis 에 set 한다.
    public void setRedisStringValue(TestChatMessage testChatMessage) {
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        stringValueOperations.set("sender", testChatMessage.getSender());
        stringValueOperations.set("context", testChatMessage.getContext());
    }

    public void getRedisStringValue(String key) {
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        System.out.println(key +" : " + stringValueOperations.get(key));
    }

    // 직접 만든 redisTemplate 사용
    // redisTemplate 를 이용해서 value 에 자바 객체를 String 으로 변환하여 저장
    public void setRedisValue(TestChatMessage testChatMessage) throws JsonProcessingException {
        String key = testChatMessage.getSender();
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(testChatMessage));
    }

    // 제대로 저장이 되었는지 확인하기 위한 부분
    // Value 가 String 으로 저장했기 때문에 인자로 넘겨준 classType 으로 변환 시키는 작업이 필요
    public <T> T  getRedisValue(String key, Class<T> classType) throws JsonProcessingException {
        String redisValue = (String)redisTemplate.opsForValue().get(key);

        return objectMapper.readValue(redisValue, classType);
    }
}
