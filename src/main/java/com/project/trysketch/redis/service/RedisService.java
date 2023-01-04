package com.project.trysketch.redis.service;

import com.project.trysketch.redis.dto.RedisToken;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RandomNickService randomNickService;

    // 랜덤 닉네임 사이트 URL
    @Value("${spring.redis.url}")
    private String url;

    // RedisToken 생성
    public void setData(Long id, String value, Long expiredTime) {
        redisTemplate.opsForValue().set(String.valueOf(id), value, expiredTime, TimeUnit.MILLISECONDS);
    }

    // RedisToken 정보 가져오기
    public String getData(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // RedisToken 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // 비회원 로그인시 발급되는 쿠키 고유번호 생성 메서드
    public Long increaseHits(Long id) {
        HashOperations<String, String, Long> hashOperations = redisTemplate.opsForHash();
        String key = String.valueOf(id);
        String hashKey = "hits";

        return hashOperations.increment(key, hashKey, 1);
    }

    // 비회원 로그인시 쿠키 발급 메서드
    public void guestLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ParseException {
        // RedisToken 만료 시간 : 1시간
        long expTime = 60 * 60 * 1000L;

        // 랜덤 닉네임 서비스 호출 및 결과 문자열로 받기
        String str = randomNickService.getData(url);

        // JSONParser 를 이용하여 JsonElement(요소) 형태로 변환하는데 사용
        // JSONParser 에 json 데이터를 넣어 파싱한 다음 JSONObject 로 변환한다.
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(str);

        // 결과물 result 문자열에 원하는 JSON 부분의 객체를 문자열로 변환해서 담는다.
        String result = String.valueOf(jsonObject.get("words"));

        // 내가 필요한것만(닉네임) 남기고 불필요한 부분들을 모두 제거해준다.
        result = result.replace("\"", "").replace("[", "").replace("]", "");

        // 새로운 redisToken 객체에 필요한 정보를 담아서 생성
        RedisToken redisToken = new RedisToken(0L, result, expTime);

        // AutoIncrement 역할을 하는 메서드를 호출하고 결과값을 새로운 변수 num 에 담는다.
        long num = increaseHits(redisToken.getId());
        setData(num, result, expTime);

        // 닉네임을 토큰에 담기 위해서 UTF-8 방식으로 인코딩
        String nickname = URLEncoder.encode(redisToken.getNickname(), StandardCharsets.UTF_8);

        // 새로운 쿠키 만들기(비회원용, 쿠키명 지정, 고유번호 입력)
        Cookie nonMemberCookie = new Cookie("non-member", String.valueOf(num));
        nonMemberCookie.setComment(nickname);           // setComment 에다가 인코딩된 유저 닉네임 넣어주기
        nonMemberCookie.setPath("/");                   // 모든 경로에서 접근 가능 하도록 설정
        response.addCookie(nonMemberCookie);            // 클라이언트에게 쿠키 전달

        // 확인용 코드
        // String test = URLDecoder.decode(nonMemberCookie.getComment(), StandardCharsets.UTF_8);
        // System.out.println("============= 유저 닉네임 디코딩 결과 ==============" + test);
    }

}
