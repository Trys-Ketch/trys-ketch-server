package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.dto.response.GameRoomResponseDto;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.redis.dto.GuestKey;
import com.project.trysketch.repository.GameRoomRepository;
import com.project.trysketch.repository.GameRoomUserRepository;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.redis.entity.Guest;
import com.project.trysketch.redis.repositorty.GuestRepository;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private final GuestRepository guestRepository;
    private static final String email = "email";                                    // 이메일 상수
    private static final String extId = "id";                                       // 유저 고유번호 상수
    private static final String extNick = "nickname";                               // 유저 닉네임 상수

    // 유저 인증부
//    public Claims authorizeToken(HttpServletRequest request){
//
//        String token = jwtUtil.resolveToken(request);
//        Claims claims;
//
//        if(token != null){
//            if (jwtUtil.validateToken(token)){
//                claims = jwtUtil.getUserInfoFromToken(token);
//                return claims;
//            }else
//                throw new CustomException(StatusMsgCode.INVALID_AUTH_TOKEN);
//        }
//        return null;
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
//    }

    // 비회원 헤더 로직
    public JSONObject guest(HttpServletRequest request) throws ParseException {
        String guestValue = request.getHeader(GuestKey.GUEST_NUM.key());            // request 안의 "guest" 라는 이름의 헤더의 값을 가져온다.
        if (guestValue != null) {
            guestValue = URLDecoder.decode(guestValue, StandardCharsets.UTF_8);     // 값이 null 이 아니면 UTF-8 형식으로 디코딩
            JSONParser jsonParser = new JSONParser();
            return (JSONObject) jsonParser.parse(guestValue);                       // Json 형식으로 변환 후 반환
        } else {
            return null;                                                            // 위에 해당하지 않을 시 null 을 반환
        }
    }

    // 회원 Id, Nickname 추출
    public HashMap<String, String> extValue(HttpServletRequest request) throws ParseException {
        Claims claims = jwtUtil.authorizeToken(request);                            // 회원 검증 로직 및 회원 정보 추출
        JSONObject guestInfo = guest(request);                                      // 비회원 정보 추출

        HashMap<String, String> result = new HashMap<>();                           // 결과물을 담기위한 HashMap

        // 회원, 비회원 분기처리 시작
        if (claims != null) {
            User user = userRepository.findByEmail(claims.get(email).toString()).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            result.put(extId, user.getId().toString());                             // 회원 Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(extNick, user.getNickname());                                // 회원 닉네임을 key 값으로 value 추출 해서 result 에 주입
        } else if (guestInfo != null) {
            Long userId = Long.parseLong(guestInfo.get(GuestKey.GUEST_NUM.key()).toString());   // guestId 를 key 값으로 value 추출
            String nickname = guestInfo.get(GuestKey.GUEST_NICK.key()).toString();              // guestId 를 key 값으로 value 추출

            Optional<Guest> guest = guestRepository.findById(userId);                           // guest 정보가 DB 에 있는지 확인(검증)
            if (!guestRepository.existsById(guest.get().getId())) {
                throw new CustomException(StatusMsgCode.INVALID_AUTH_TOKEN);
            }
            result.put(extId, userId.toString());                                   // guest Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(extNick, nickname);                                          // guest 닉네임을 key 값으로 value 추출 해서 result 에 주입
        }
        return result;
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
    public DataMsgResponseDto createGameRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request) throws ParseException {
/*        // 1. 메서드를 이용해서 회원, 비회원 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        JSONObject guestInfo = guest(request);

        // 2. 공통적으로 사용되는 부분을 미리 선언
        Long userId = null;
        String nickname = null;

        // 3. 분기처리를 통해서 회원, 비회원에 해당하는 경우로 수행
        if (claims != null) {
            // 4. 회원의 경우 로직 수행
            User user = userRepository.findByEmail(claims.get(email).toString()).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            userId = user.getId();
            nickname = user.getNickname();
        } else if (guestInfo != null) {
            // 5. 비회원의 경우 로직 수행
            userId = Long.valueOf(guestInfo.get(GuestKey.GUEST_NUM.key()).toString()); // guest PK 를 key 값을 통해서 추출
            nickname = guestInfo.get(GuestKey.GUEST_NICK.key()).toString();            // guest nickname 을 key 값을 통해서 추출

            Optional<Guest> guest = guestRepository.findById(userId);                  // guest 정보가 DB 에 있는지 확인(검증)
            if (!guestRepository.existsById(guest.get().getId())) {
                throw new CustomException(StatusMsgCode.INVALID_AUTH_TOKEN);
            }
        }*/
        // 1. 받아온 헤더로부터 유저 또는 guest 정보를 받아온다.
        HashMap<String, String> extInfo = extValue(request);

        // 2. 요청을 한 유저가 이미 속한 방이 있으면 생성 불가능
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(extId)))) {
            throw new CustomException(StatusMsgCode.ONE_MAN_ONE_ROOM);
        }

        // 3. 방 정보 생성
        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomRequestDto.getTitle())
                .host(extInfo.get(extNick))
                .status("false")
                .build();

        // 4. 방에 입장한 유저 정보 생성
        GameRoomUser gameRoomUser = GameRoomUser.builder()
                .gameRoom(gameRoom)
                .userId(Long.valueOf(extInfo.get(extId)))
                .nickname(extInfo.get(extNick))
                .webSessionId(null)
                .build();

        // 5. 게임 방 DB에 저장 및 입장중인 유저 정보 저장
        gameRoomRepository.save(gameRoom);
        gameRoomUserRepository.save(gameRoomUser);

        HashMap<String, String> roomInfo = new HashMap<>();

        // 6. HashMap 형식으로 방 제목과 방 번호를 response 로 반환
        roomInfo.put("gameRoomtitle",gameRoom.getTitle());
        roomInfo.put("roomId", String.valueOf(gameRoom.getId()));

        return new DataMsgResponseDto(StatusMsgCode.OK,roomInfo);
    };

    // 게임방 입장
    @Transactional
    public MsgResponseDto enterGameRoom(Long id, HttpServletRequest request) throws ParseException {
        // 1. 받아온 헤더로부터 유저 또는 guest 정보를 받아온다.
        HashMap<String, String> extInfo = extValue(request);

        // 2. id로 DB 에서 현재 들어갈 게임방 데이터 찾기
        Optional<GameRoom> enterGameRoom = gameRoomRepository.findById(id);

        // 3. 게임 방의 상태가 true 이면 게임이 시작중이니 입장불가능
        if (enterGameRoom.get().getStatus().equals("true")){
            return new MsgResponseDto(StatusMsgCode.ALREADY_PLAYING);
        }

        // 4. 현재 방의 인원이 8명 이상이면 풀방임~
        Long checkUsers = gameRoomUserRepository.countByGameRoomIdOrderByUserId(enterGameRoom.get().getId());
        if (checkUsers >= 8) {
            return new MsgResponseDto(StatusMsgCode.FULL_BANG);
        }

        // 5. 현재 User 가 다른 방에 들어가 있다면
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(extId)))) {
            return new MsgResponseDto(StatusMsgCode.ONE_MAN_ONE_ROOM);
        }

        // 6. 새롭게 게임방에 들어온 유저 생성
        GameRoomUser gameRoomUser = GameRoomUser.builder()
                .gameRoom(enterGameRoom.get())
                .userId(Long.valueOf(extInfo.get(extId)))
                .nickname(extInfo.get(extNick))
                .webSessionId(null)
                .build();

        // 7. 게임방에 들어온 유저를 DB에 저장
        gameRoomUserRepository.save(gameRoomUser);

        return new MsgResponseDto(StatusMsgCode.SUCCESS_ENTER_GAME);
    }

    // 게임방 나가기
    @Transactional
    public MsgResponseDto exitGameRoom(Long id, HttpServletRequest request) throws ParseException {
        HashMap<String, String> extInfo = new HashMap<>();
        if (request != null) {
            extInfo = extValue(request);
        }
//            GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebsessionId(userUUID);
//            extInfo.put(extId, gameRoomUser.getUserId().toString());
//            extInfo.put(extNick, gameRoomUser.getNickname());
//            id = gameRoomUser.getGameRoom().getId();


//        result.put(extId, userId.toString());                                   // guest Id 를 key 값으로 value 추출 해서 result 에 주입
//            result.put(extNick, nickname);

        // 나가려고 하는 GameRoom 정보 가져오기
        GameRoom enterGameRoom = gameRoomRepository.findById(id).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 나가려고 하는 GameRoomUser 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserId(Long.valueOf(extInfo.get(extId)));

        // 해당 user 를 GameRoomUser 에서 삭제
        gameRoomUserRepository.delete(gameRoomUser);

        // 방장이 나간 방의 UserList 정보 가져오기
        List<GameRoomUser> leftGameRoomUserList = gameRoomUserRepository.findByGameRoom(enterGameRoom);

        // 게임 방의 남은 인원이 0명이면 게임 방도 삭제
        if (leftGameRoomUserList.size() == 0){
            gameRoomRepository.delete(enterGameRoom);
        }

        // 나간 User 와 해당 GameRoom 의 방장이 같다면 && GameRoom 에 User 가 없지 않다면
        if (extInfo.get(extNick).equals(enterGameRoom.getHost()) && !leftGameRoomUserList.isEmpty()){

            // 게임 방 유저들중 현재 방장 다음으로 들어온 UserId 가져오기
            Long newHostId = leftGameRoomUserList.get(0).getId();

            // UserId 를 들고 GameRoomUser 정보 가져오기
            GameRoomUser newHost = gameRoomUserRepository.findById(newHostId).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            Optional<User> newHostInfo = userRepository.findById(newHost.getId());

            // 새로운 Host 가 선정되어 GameRoom 정보 빌드
            GameRoom updateGameRoom = GameRoom.builder()
                    .id(enterGameRoom.getId())
                    .host(newHostInfo.get().getNickname())
                    .title(enterGameRoom.getTitle())
                    .status("false")
                    .build();

            // 기존 GameRoom 에 새로 빌드된 GameRoom 정보 업데이트
            gameRoomRepository.save(updateGameRoom);
        }
        return new MsgResponseDto(StatusMsgCode.SUCCESS_EXIT_GAME);
    }

    // 웹소켓 연결이 헤제되면 게임방 나가기
    @Transactional
    public void exitGameRoom(String userUUID) {

        // extInfo 선언
        HashMap<String, String> extInfo = new HashMap<>();

        // userUUID로 해당 GameRoomUser 정보가져오기
        GameRoomUser gameRoomUser1 = gameRoomUserRepository.findByWebsessionId(userUUID);

        // extValue 메소드 호출X 직접 입력
        extInfo.put(extId, gameRoomUser1.getUserId().toString());
        extInfo.put(extNick, gameRoomUser1.getNickname());

        // GameRoom 정보 가져오기
        Long id = gameRoomUser1.getGameRoom().getId();


//        result.put(extId, userId.toString());                                   // guest Id 를 key 값으로 value 추출 해서 result 에 주입
//            result.put(extNick, nickname);

        // 나가려고 하는 GameRoom 정보 가져오기
        GameRoom enterGameRoom = gameRoomRepository.findById(id).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 나가려고 하는 GameRoomUser 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserId(Long.valueOf(extInfo.get(extId)));

        // 해당 user 를 GameRoomUser 에서 삭제
        gameRoomUserRepository.delete(gameRoomUser);

        // 방장이 나간 방의 UserList 정보 가져오기
        List<GameRoomUser> leftGameRoomUserList = gameRoomUserRepository.findByGameRoom(enterGameRoom);

        // 게임 방의 남은 인원이 0명이면 게임 방도 삭제
        if (leftGameRoomUserList.size() == 0){
            gameRoomRepository.delete(enterGameRoom);
        }

        // 나간 User 와 해당 GameRoom 의 방장이 같다면 && GameRoom 에 User 가 없지 않다면
        if (extInfo.get(extNick).equals(enterGameRoom.getHost()) && !leftGameRoomUserList.isEmpty()){

            // 게임 방 유저들중 현재 방장 다음으로 들어온 UserId 가져오기
            Long newHostId = leftGameRoomUserList.get(0).getId();

            // UserId 를 들고 GameRoomUser 정보 가져오기
            GameRoomUser newHost = gameRoomUserRepository.findById(newHostId).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            Optional<User> newHostInfo = userRepository.findById(newHost.getId());

            // 새로운 Host 가 선정되어 GameRoom 정보 빌드
            GameRoom updateGameRoom = GameRoom.builder()
                    .id(enterGameRoom.getId())
                    .host(newHostInfo.get().getNickname())
                    .title(enterGameRoom.getTitle())
                    .status("false")
                    .build();

            // 기존 GameRoom 에 새로 빌드된 GameRoom 정보 업데이트
            gameRoomRepository.save(updateGameRoom);
        }
    }

    @Transactional
    public void websessionIdUpate(Long gameRoomId, String token, String userUUID) {
        // 유저 정보 인증부
        Claims claims = jwtUtil.authorizeToken1(token);
        Long userId = (Long) claims.get("id");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 해당 User 데이터로 GameRoomUser 데이터 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(user.getId(), gameRoomId);

        // 해당 GameRoomUser 업데이트
        GameRoomUser updateGameRoomUser = GameRoomUser.builder()
                .id(gameRoomUser.getId())
                .gameRoom(gameRoomUser.getGameRoom())
                .userId(gameRoomUser.getUserId())
                .nickname(gameRoomUser.getNickname())
                .webSessionId(userUUID)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllGameRoomUsersExceptMe(Long roomId, String userUUID){

        // GameRoomId 데이터로 GameRoomUser 데이터의 List 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(roomId);

        // [ { id : userUUID1 }, { id : userUUID1 }, ...  ]
        // 해당 방에 본인을 제외한 전체 유저 리스트 생성
        List<Map<String, String>> originGameRoomUserUUIDList = new ArrayList<>();

        for (GameRoomUser gameRoomUser : gameRoomUserList){

            // GameRoomUser 의 WebSessionId 가져오기
            String userSessionId = gameRoomUser.getWebSessionId();

            // 만약 가져온 WebSessionId 와 본인( userUUID ) 가 같지않다면
            if (!userSessionId.equals(userUUID)) {
                // userMap 선언
                Map<String, String> userMap = new HashMap<>();

                // 본인을 제외한 GameRoomUser 의 WebSessionId 추가
                userMap.put("Id", userSessionId);

                // userMap 을 List 의 객체로 추가
                originGameRoomUserUUIDList.add(userMap);
            }
        }
        return originGameRoomUserUUIDList;
    }

}
