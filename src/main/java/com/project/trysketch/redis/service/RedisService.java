package com.project.trysketch.redis.service;

import com.project.trysketch.redis.dto.StaticString;
import com.project.trysketch.redis.entity.Guest;
import com.project.trysketch.redis.dto.GuestNickRequestDto;
import com.project.trysketch.redis.repositorty.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 1. 기능   : Redis 비즈니스 로직
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final GuestRepository guestRepository;

    // 비회원 로그인시 발급되는 자동값증가 메서드
    public Long incTest(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // 쿠키 사용법 : https://goodwoong.tistory.com/125
    // 비회원 로그인시 헤드 추가 메서드
//    @Cacheable(key = "#result", value = "Default24h", cacheManager = "defaultCacheManager")
    public void guestLogin(HttpServletRequest request, HttpServletResponse response, GuestNickRequestDto requestDto) {
        String nickname = requestDto.getNickname();     // 클라이언트로 부터 받아오는 닉네임

        Long num = incTest("guestCount");           // 자동값 증가 키값 지정 및 시작
        Long result = 10000L + num;                     // 10000 번 부터 시작해서 1씩 증가(첫번째 값 10001)

        Guest guest = new Guest(result, nickname);      // 새로운 guest 객체에 필요한 정보를 담아서 생성
        guestRepository.save(guest);                    // DB 에 저장

        // Json 타입으로 변환하기 위해서 문자열 형태로 만들어 줌
        String test = String.format("{\"guest\":\"%d\", \"nickname\":\"%s\"}", result, nickname);

        // Json 형태로 만든 문자열을 헤더에 넣기 위해서 UTF-8 으로 인코딩
        String encodeResult = URLEncoder.encode(test, StandardCharsets.UTF_8);

        // 헤더에 헤더값 지정 및 바디 값 넣어주기
        response.addHeader(StaticString.guestKey, encodeResult);

        // 디코딩 확인용 코드입니다. 최종적으로는 삭제하면 됩니다.
        System.out.println("디코딩 결과 : " + URLDecoder.decode(test, StandardCharsets.UTF_8));

    }

    public void delTest(Long guests) {
        guestRepository.deleteById(guests);
    }
}
