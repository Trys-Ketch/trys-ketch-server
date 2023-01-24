package com.project.trysketch.service;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.entity.Guest;
import com.project.trysketch.dto.request.GuestNickRequestDto;
import com.project.trysketch.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 1. 기능   : Redis 비즈니스 로직
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class GuestService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GuestRepository guestRepository;

    // 비회원 로그인시 발급되는 자동값증가 메서드
    public Long guestIncrement(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // 비회원 로그인시 헤드 추가 메서드
    public MsgResponseDto guestLogin(HttpServletResponse response, GuestNickRequestDto requestDto) {
        Long num = guestIncrement("guestCount");    // 자동값 증가 키값 지정 및 시작
        Long guestId = 10000L + num;                    // 10000 번 부터 시작해서 1씩 증가(첫번째 값 10001)

        Guest guest = new Guest(guestId
                , guestId.toString()
                , requestDto.getNickname()
                , requestDto.getImgUrl());              // 새로운 guest 객체에 필요한 정보를 담아서 생성
        guestRepository.save(guest);                    // DB 에 저장

        // 쉼표로 구분되는 문자열 형태로 만들어 줌
        String guestStr = String.format("%d,%s,%s", guestId, guest.getNickname(), guest.getImgUrl());

        // 위에서 만든 문자열을 헤더에 넣기 위해서 UTF-8 으로 인코딩
        String encodeResult = URLEncoder.encode(guestStr, StandardCharsets.UTF_8);

        // 헤더에 헤더값 지정 및 바디 값 넣어주기
        response.addHeader("guest", encodeResult);

        // 디코딩 확인용 코드입니다. 최종적으로는 삭제하면 됩니다.
        System.out.println("디코딩 결과 : " + URLDecoder.decode(guestStr, StandardCharsets.UTF_8));

        return new MsgResponseDto(StatusMsgCode.LOG_IN);
    }

}