package com.project.trysketch.gameroom.service;

import com.project.trysketch.gameroom.dto.request.GameRoomRequestDto;
import com.project.trysketch.gameroom.dto.response.GameRoomCreateResponseDto;
import com.project.trysketch.gameroom.entity.GameRoom;
import com.project.trysketch.gameroom.entity.GameRoomUser;
import com.project.trysketch.gameroom.repository.GameRoomRepository;
import com.project.trysketch.gameroom.repository.GameRoomUserRepository;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.user.entity.User;
import com.project.trysketch.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

// 1. 기능   : 프로젝트 메인 로직
// 2. 작성자 : 김재영
@Slf4j
@RequiredArgsConstructor
@Service
public class GameRoomService {
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomUserRepository gameRoomUserRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 유저 인증부
    public Claims authorizeToken(HttpServletRequest request){

        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if(token != null){
            if (jwtUtil.validateToken(token)){
                claims = jwtUtil.getUserInfoFromToken(token);
                return claims;
            }else
                throw new CustomException(StatusMsgCode.INVALID_AUTH_TOKEN);
        }
        return null;
//        //request 의 헤더에서 Authorization 확인
//        if(request.getHeader("Authorization") == null){
//            throw new IllegalArgumentException("만료된 토큰");
//        }
//
//        if (!jwtUtil.validateToken(request.getHeader("Authorization"))){
//            throw new IllegalArgumentException("만료된 토큰");
//        }
//
//        User auth_user = jwtUtil.getUserFromAuthentication();

    }

    // 게임방 조회


    // 게임방 생성
    @Transactional
    public GameRoomCreateResponseDto createGameRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request){

        Claims claims = authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomRequestDto.getTitle())
                .host(user.getNickname())
                .status("false")
                .build();

        // DB에 저장
        gameRoomRepository.save(gameRoom);

        GameRoomUser gameRoomUser = new GameRoomUser(gameRoom,user);
        gameRoomUserRepository.save(gameRoomUser);

        HashMap<String, String> roomInfo = new HashMap<>();

        roomInfo.put("gameRoomtitle",gameRoom.getTitle());
        roomInfo.put("roomId", String.valueOf(gameRoom.getId()));

        return new GameRoomCreateResponseDto(StatusMsgCode.OK,roomInfo);
    };

    // 게임방 입장
    @Transactional
    public MsgResponseDto enterGameRoom(Long id, HttpServletRequest request) {
        Claims claims = authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // id로 DB 에서 현재 들어갈 게임방 데이터 찾기
        Optional<GameRoom> entergameRoom = gameRoomRepository.findById(id);

        // 게임 방의 상태가 true 이면 게임이 시작중이니 입장불가능
        if (entergameRoom.get().getStatus().equals("true")){
            return new MsgResponseDto(StatusMsgCode.ALREADY_PLAYING);
        }

        // 현재 방의 유저 리스트를 받아옴
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findByGameRoom(entergameRoom);

        // 현재 방의 인원이 8명 이상이면 풀방임~
        if (gameRoomUserList.size() >= 8){
            return new MsgResponseDto(StatusMsgCode.FULL_BANG);
        }

        // 이미 방에 들어온 유저의 재입장 불가
        for (GameRoomUser gameRoomUser : gameRoomUserList){
            Optional<User> ingameUser = userRepository.findById(gameRoomUser.getUser().getId());
            if (user.getId() == ingameUser.get().getId()){
                return new MsgResponseDto(StatusMsgCode.DUPLICATE_USER);
            }
        }

        GameRoomUser gameRoomUser = new GameRoomUser(entergameRoom,user);

        gameRoomUserRepository.save(gameRoomUser);

        return new MsgResponseDto(StatusMsgCode.OK);
    }


}
