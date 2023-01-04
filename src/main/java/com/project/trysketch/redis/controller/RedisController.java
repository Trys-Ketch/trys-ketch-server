package com.project.trysketch.redis.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.trysketch.redis.test.TestChatMessage;
import com.project.trysketch.redis.test.TestRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisController {

    private final TestRedisService testRedisService;

    // JSON 형태로 객체를 전달받으면 set/get 을 한번에 실행해서 최종적으로 출력
    @PostMapping("/api/redisStringTest")
    public String sendString(@RequestBody TestChatMessage testChatMessage) {
        testRedisService.setRedisStringValue(testChatMessage);

        testRedisService.getRedisStringValue("sender");
        testRedisService.getRedisStringValue("context");

        return "success";
    }

    @PostMapping("/api/redisTest")
    public String send(@RequestBody TestChatMessage testChatMessage) throws JsonProcessingException {
        testRedisService.setRedisValue(testChatMessage);

        String key = testChatMessage.getSender();
        TestChatMessage testChatMessage1 = testRedisService.getRedisValue(key, TestChatMessage.class);

        return testChatMessage1.getContext();
    }
}
