package com.project.trysketch.global.rtc;

import com.fasterxml.jackson.databind.ObjectMapper;

// 1. 기능   : ObjectMapper 관련 메서드 제공
// 2. 작성자 : 안은솔

public class Utils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // JSON -> Java Objcet (deserialization)
    public static Message getObject(final String message) throws Exception {
        return objectMapper.readValue(message, Message.class);
    }

    // Java Object -> JSON (serialization)
    public static String getString(final Message message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}
