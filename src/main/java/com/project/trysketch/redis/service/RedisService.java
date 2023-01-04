package com.project.trysketch.redis.service;

import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.redis.dto.RedisTokenDto;
import com.project.trysketch.redis.repositorty.RedisAccessTokenRepository;
import com.project.trysketch.redis.service.RandomNickService;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisAccessTokenRepository redisAcTokenRepository;
    private final JwtUtil jwtUtil;
    private final RandomNickService randomNickService;

    Long num = 0L;

    public void setData(Long id, String value, Long expiredTime) {
        redisTemplate.opsForValue().set(String.valueOf(id), value, expiredTime, TimeUnit.MILLISECONDS);
    }

    public String getData(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    public void guestLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ParseException {
        num++;
        long expTime = 60 * 60 * 1000L;

        String str = "";
        str = randomNickService.getData("https://nickname.hwanmoo.kr/?format=json&count=1");

        JSONParser jsonParser = new JSONParser();
        Object obj = jsonParser.parse(str);
        JSONObject jsonObject = (JSONObject) obj;
        String test = String.valueOf(jsonObject.get("words"));

        String result = test.replace("\"", "").replace("[", "").replace("]", "");

        String check = testBase64Encode(result);

        RedisTokenDto redisTokenDto = new RedisTokenDto(num, result, expTime);
        setData(redisTokenDto.getId(), result, expTime);

        // base64 인코딩, 디코딩 테스트
        System.out.println("인코딩 값 : " + check + "\n디코딩 값 : " + testBase64Decode(check));

        response.addHeader("redisToken", check);
    }

    public String testBase64Encode(String content) {
        return Base64Utils.encodeToString(content.getBytes());
    }

    public String testBase64Decode(String content) {
        return new String(Base64Utils.decode(content.getBytes()));
    }

}
