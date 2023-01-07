package com.project.trysketch.gameroom.service;

import com.project.trysketch.gameroom.dto.request.GameRoomRequestDto;
import com.project.trysketch.gameroom.dto.response.GameRoomResponseDto;
import com.project.trysketch.global.dto.DataMsgResponseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
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
    @Transactional //전체 list, 각 방 title, 각 방 인원, 각 방 시작상태 반환할 것
    public List<GameRoomResponseDto> getAllGameRoom(Pageable pageable) {
        Page<GameRoom> rooms = gameRoomRepository.findAll(pageable);

        List<GameRoomResponseDto> gameRoomList = new ArrayList<>();
        for (GameRoom gameRoom : rooms){

            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                                .id(gameRoom.getId())
                                .title(gameRoom.getTitle())
                                .host(gameRoom.getHost())
                                .GameRoomUserCount(gameRoom.getGameRoomUserList().size())
                                .status(gameRoom.getStatus())
                                .createdAt(gameRoom.getCreatedAt())
                                .modifiedAt(gameRoom.getModifiedAt())
                                .build();
            gameRoomList.add(gameRoomResponseDto);
        }
        return gameRoomList;
    }

    // 게임방 생성
    @Transactional
    public DataMsgResponseDto createGameRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request){

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

        return new DataMsgResponseDto(StatusMsgCode.OK,roomInfo);
    };

    // 게임방 입장
    @Transactional
    public MsgResponseDto enterGameRoom(Long id, HttpServletRequest request) {
        Claims claims = authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // id로 DB 에서 현재 들어갈 게임방 데이터 찾기
        Optional<GameRoom> enterGameRoom = gameRoomRepository.findById(id);

        // 게임 방의 상태가 true 이면 게임이 시작중이니 입장불가능
        if (enterGameRoom.get().getStatus().equals("true")){
            return new MsgResponseDto(StatusMsgCode.ALREADY_PLAYING);
        }

        // 현재 방의 유저 리스트를 받아옴
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findByGameRoom(enterGameRoom);

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
        
        // 새롭게 게임방에 들어온 유저 생성
        GameRoomUser gameRoomUser = new GameRoomUser(enterGameRoom,user);

        // 게임방에 들어온 유저를 DB에 저장
        gameRoomUserRepository.save(gameRoomUser);

        return new MsgResponseDto(StatusMsgCode.SUCCESS_ENTER_GAME);
    }

    //게임방 나가기
    @Transactional
    public MsgResponseDto exitGameRoom(Long id, HttpServletRequest request) {

        // 나갈려는 User 정보 가져오기
        Claims claims = authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 나가려고 하는 GameRoom 정보 가져오기
        GameRoom enterGameRoom = gameRoomRepository.findById(id).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 나가려고 하는 GameRoomUser 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUser(user);

        // 해당 user 를 GameRoomUser 에서 삭제
        gameRoomUserRepository.delete(gameRoomUser);

        // 방장이 나간 방의 UserList 정보 가져오기
        List<GameRoomUser> leftGameRoomUserList = gameRoomUserRepository.findByGameRoom(enterGameRoom);

        // 게임 방의 남은 인원이 0명이면 게임 방도 삭제
        if (leftGameRoomUserList.size() ==0){
            gameRoomRepository.delete(enterGameRoom);
        }

        // 나간 User 와 해당 GameRoom 의 방장이 같다면 && GameRoom 에 User 가 없지 않다면
        if (user.getNickname().equals(enterGameRoom.getHost()) && !leftGameRoomUserList.isEmpty()){
//            Long newHostId = leftGameRoomUserList.get((int) (Math.random()*leftGameRoomUserList.size())).getId();

            // 게임 방 유저들중 현재 방장 다음으로 들어온 UserId 가져오기
            Long newHostId = leftGameRoomUserList.get(0).getId();

            // UserId 를 들고 GameRoomUser 정보 가져오기
            GameRoomUser newHost = gameRoomUserRepository.findById(newHostId).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );

            // 새로운 Host 가 선정되어 GameRoom 정보 빌드
            GameRoom updateGameRoom = GameRoom.builder()
                    .id(enterGameRoom.getId())
                    .host(newHost.getUser().getNickname())
                    .title(enterGameRoom.getTitle())
                    .status("false")
                    .build();

            // 기존 GameRoom 에 새로 빌드된 GameRoom 정보 업데이트
            gameRoomRepository.save(updateGameRoom);
        }
        return new MsgResponseDto(StatusMsgCode.SUCCESS_EXIT_GAME);
    }
}
