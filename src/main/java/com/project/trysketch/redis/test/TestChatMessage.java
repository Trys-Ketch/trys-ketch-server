package com.project.trysketch.redis.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestChatMessage {
    private String sender;
    private String context;
}
