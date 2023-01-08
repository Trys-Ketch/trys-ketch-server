package com.project.trysketch.redis.service;

import com.project.trysketch.redis.dto.TestRoomUsersDto;
import com.project.trysketch.redis.entity.NonMember;
import com.project.trysketch.redis.dto.NonMemberNickRequestDto;
import com.project.trysketch.redis.entity.RoomUsers;
import com.project.trysketch.redis.repositorty.RoomUsersRepository;
import com.project.trysketch.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// 1. 기능   : Redis 비즈니스 로직
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RoomUsersRepository roomUsersRepository;
    private final UserService userService;

    public static final String MEMBER_HEADER ="non-member";

    // 랜덤 닉네임 사이트 URL
    @Value("${spring.redis.url}")
    private String url;

    // RedisToken 생성
    public void setNonMember(String id, String value, Long expiredTime) {
        redisTemplate.opsForValue().set("non:" + id, value, expiredTime, TimeUnit.MILLISECONDS);
    }

    public void setRoomUsers () {

    }

    // RedisToken 정보 가져오기
    public String getData(String id) {
        return (String) redisTemplate.opsForValue().get(id);
    }

    //
    public void getRoomUsers(Long id) {
        return;
    }

    // RedisToken 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // 비회원 로그인시 발급되는 쿠키 고유번호 생성 메서드
//    public void increaseHits(Long id, String name) {
//        HashOperations<String, String, Long> hashOperations = redisTemplate.opsForHash();
//        String key = String.valueOf(id);
//
//        hashOperations.increment(key, name, 1);
//    }

    public Long incTest(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // 쿠키 사용법 : https://goodwoong.tistory.com/125
    // 비회원 로그인시 쿠키 발급 메서드
    public void guestLogin(HttpServletRequest request, HttpServletResponse response, NonMemberNickRequestDto requestDto) {

        // RedisToken 만료 시간 : 1시간
        long expTime = 60 * 60 * 1000L;

        // 클라이언트로 부터 받아오는 닉네임
        String nickname = requestDto.getNickname();

        Long num = incTest("nonMember");
        // 새로운 redisToken 객체에 필요한 정보를 담아서 생성
        NonMember nonMember = new NonMember(num, nickname, expTime);

        // AutoIncrement 역할을 하는 메서드를 호출하고 결과값을 새로운 변수 num 에 담는다.
        setNonMember(String.valueOf(num), nickname, expTime);

        // 닉네임을 토큰에 담기 위해서 UTF-8 방식으로 인코딩
//        nickname = URLEncoder.encode(nonMember.getNickname(), StandardCharsets.UTF_8);
//        System.out.println(getData(String.valueOf(num)));

        String test = "";
        test = String.format("{\"nonMember\":\"%d\", \"nickname\":\"%s\"}", num, nickname);

        String out = URLEncoder.encode(test, StandardCharsets.UTF_8);

        response.addHeader(MEMBER_HEADER, out);

        System.out.println("디코딩 결과 : " + URLDecoder.decode(test, StandardCharsets.UTF_8));
        System.out.println("getData 결과 : " + getData("non"));

        // 확인용 코드
        // String test = URLDecoder.decode(nonMemberCookie.getComment(), StandardCharsets.UTF_8);
        // System.out.println("============= 유저 닉네임 디코딩 결과 ==============" + test);
    }

    public void roomUsersTest(TestRoomUsersDto dto) {
        long expTime = 20 * 1000L;

        Long num = incTest("room");
        RoomUsers roomUsers = RoomUsers.builder()
                .id(num)
                .roomNum(dto.getRoomNum())
                .nickname(dto.getNickname())
                .readyStatus(dto.isReadyStatus())
                .build();

        roomUsersRepository.save(roomUsers);

        redisTemplate.expire("roomUsers:" + num, expTime, TimeUnit.MILLISECONDS);
    }

}
