package com.project.trysketch.service;

import com.project.trysketch.entity.ChatMessage;
import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.dto.response.GameRoomResponseDto;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.dto.GamerEnum;
import com.project.trysketch.repository.GameRoomRepository;
import com.project.trysketch.repository.GameRoomUserRepository;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.utill.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

// 1. 기능   : 프로젝트 메인 로직
// 2. 작성자 : 김재영, 서혁수, 안은솔, 황미경
// 3. 참고사항 : 회원, 비회원 정보는 모듈화를 통해 userService 에서 가져옵니다.
@Slf4j
@RequiredArgsConstructor
@Service
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomUserRepository gameRoomUserRepository;
    private final GameService gameService;
    private final UserService userService;
    private final SseEmitters sseEmitters;
    private final SseService sseService;
    private final SimpMessageSendingOperations sendingOperations;

    // ============================== 게임방 조회 ==============================
    @Transactional
    public Map<String, Object> getAllGameRoom(Pageable pageable) {
        // pageable 객체로 GameRoom 을 Page<> 에 담아 가져오기
        Page<GameRoom> gameRoomPage = gameRoomRepository.findAll(pageable);

        // GameRoom 을 Dto 형태로 담아줄 List 선언
        List<GameRoomResponseDto> gameRoomList = new ArrayList<>();

        // GameRoom 의 정보와 LastPage 정보를 담아줄 Map 선언
        Map<String, Object> getAllGameRoom = new HashMap<>();

        for (GameRoom gameRoom : gameRoomPage){

            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(gameRoom.getId())
                    .title(gameRoom.getTitle())
                    .hostNick(gameRoom.getHostNick())
                    .GameRoomUserCount(gameRoom.getGameRoomUserList().size())
                    .isPlaying(gameRoom.isPlaying())
                    .createdAt(gameRoom.getCreatedAt())
                    .modifiedAt(gameRoom.getModifiedAt())
                    .randomCode(gameRoom.getRandomCode())
                    .build();
            gameRoomList.add(gameRoomResponseDto);
        }
        getAllGameRoom.put("Rooms", gameRoomList);
        getAllGameRoom.put("LastPage",gameRoomPage.getTotalPages());

        return getAllGameRoom;
    }

    // ============================ 게임방 상세 조회 ============================
    @Transactional(readOnly = true)
    public GameRoomResponseDto getGameRoom(Long id, HttpServletRequest request) {
        // 받아온 헤더로부터 유저 또는 guest 정보를 받아온다
        String header = userService.validHeader(request);
        HashMap<String, String> extInfo = userService.getGamerInfo(header);

        // 받아온 roomId 로 gameRoom 정보를 받아온다
        GameRoom gameRoom = gameRoomRepository.findById(id).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // Dto 형태로 반환
        return GameRoomResponseDto.builder()
                .title(gameRoom.getTitle())
                .randomCode(gameRoom.getRandomCode()).build();
    }

    // ============================== 게임방 생성 ==============================
    @Transactional
    public HashMap<String, String> createGameRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request) {
        // 받아온 헤더로부터 유저 또는 guest 정보를 받아온다
        String header = userService.validHeader(request);
        HashMap<String, String> extInfo = userService.getGamerInfo(header);

        // 혹시 모를 오류상황으로 현재 User 가 다른 방에 들어가 있다면 gameRoomUser 삭제
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))) {
            gameRoomUserRepository.deleteByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())));
        }

        // 초대 코드를 위한 랜덤코드 생성
        String randomCode = createRandomCode();

        // 방 정보 생성
        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomRequestDto.getTitle())
                .hostId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))
                .hostNick(extInfo.get(GamerEnum.NICK.key()))
                .isPlaying(false)
                .randomCode(randomCode)
                .build();

        // 방에 입장한 유저 정보 생성
        GameRoomUser gameRoomUser = GameRoomUser.builder()
                .gameRoom(gameRoom)
                .userId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))
                .nickname(extInfo.get(GamerEnum.NICK.key()))
                .imgUrl(extInfo.get(GamerEnum.IMG.key()))
                .webSessionId(null)
                .readyStatus(true)
                .build();

        log.info(">>>>>>>>>> 접속한 유저의 정보를 토대로 gameRoomUser 생성 {}", gameRoomUser);
        log.info(">>>>>>>>>> true 여야 함 {}", gameRoomUser.isReadyStatus());

        // 게임 방 DB에 저장 및 입장중인 유저 정보 저장
        gameRoomRepository.save(gameRoom);
        gameRoomUserRepository.save(gameRoomUser);

        // HashMap 형식으로 방 번호를 response 로 반환
        HashMap<String, String> roomIdInfo = new HashMap<>();
        roomIdInfo.put("roomId", String.valueOf(gameRoom.getId()));

        // SSE event 생성
        sseEmitters.changeRoom(sseService.getRooms(1));

        return roomIdInfo;
    }

    // ============================ 랜덤 코드 생성부 ============================
    @Transactional
    public String createRandomCode() {
        // 초대 코드를 위한 랜덤코드 생성
        StringBuilder key = new StringBuilder();
        Random rnd = new Random();

        // 소문자와 숫자 8자리 조합 만들기
        for (int i = 0; i < 8; i++) {
            int index = rnd.nextInt(2); // 0~1 중 랜덤

            switch (index) {
                case 0 -> key.append((char) ((rnd.nextInt(26)) + 97));  //  a~z  (ex. 1+97=98 => (char)98 = 'b')
                case 1 -> key.append((rnd.nextInt(10)));                // 0~9
            }
        }
        return key.toString();
    }

    // ============================ 초대 게임방 입장 ============================
    @Transactional
    public HashMap<String, Object> enterGameRoom(String randomCode, HttpServletRequest request) {
        // 받아온 헤더로부터 유저 또는 guest 정보를 받아온다.
        String header = userService.validHeader(request);
        HashMap<String, String> extInfo = userService.getGamerInfo(header);

        // id로 DB 에서 현재 들어갈 게임방 데이터 찾기
        GameRoom enterGameRoom = gameRoomRepository.findByRandomCode(randomCode).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 게임 방의 상태가 true 이면 게임이 시작중이니 입장 불가능
        if (enterGameRoom.isPlaying()){
            throw new CustomException(StatusMsgCode.ALREADY_PLAYING);
        }

        // 현재 방의 인원이 8명 이상이면 풀방임~
        Long gameRoomUserCount = gameRoomUserRepository.countByGameRoomIdOrderByUserId(enterGameRoom.getId());
        if (gameRoomUserCount >= 8) {
            throw new CustomException(StatusMsgCode.FULL_BANG);
        }

        // 혹시 모를 오류상황으로 현재 User 가 다른 방에 들어가 있다면 gameRoomUser 삭제
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))) {
            gameRoomUserRepository.deleteByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())));
        }

        // 새롭게 게임방에 들어온 유저 생성
        GameRoomUser gameRoomUser = GameRoomUser.builder()
                .gameRoom(enterGameRoom)
                .userId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))
                .nickname(extInfo.get(GamerEnum.NICK.key()))
                .imgUrl(extInfo.get(GamerEnum.IMG.key()))
                .webSessionId(null)
                .readyStatus(false)
                .build();

        // 게임방에 들어온 유저를 DB에 저장
        gameRoomUserRepository.save(gameRoomUser);

        // HashMap 형식으로 방 번호를 response 로 반환
        HashMap<String, Object> roomIdInfo = new HashMap<>();
        roomIdInfo.put("roomId", enterGameRoom.getId());

        // SSE event 생성
        sseEmitters.changeRoom(sseService.getRooms(1));

        return roomIdInfo;
    }

    // ============================= 게임방 나가기 ==============================
    @Transactional
    public void exitGameRoom(String webSessionId, Long gameRoomId) {
        // 유저의 sessionID로 gameRoomUser 정보, 유저가 있는 gameRoom 불러오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(webSessionId).orElse(null);
        if (gameRoomUser == null) {
            // gameRoomUser 조회 결과가 null 즉, 강퇴로 인해 유저정보가 이미 없어진 상태
            log.info(">>>>>>> 위치 : GameRoomService 의 exitGameRoom 메서드 / 이미 강퇴된 상태면 아무런 로직 실행 없이 null 값을 반환");
            return;
        } else {
            GameRoom currentGameRoom = gameRoomUser.getGameRoom();

            // 게임이 진행중이며, 나가는 유저 포함 3명 이하일 때 게임종료 (3명 미만으로는 게임진행 불가)
            if(currentGameRoom.isPlaying() && currentGameRoom.getGameRoomUserList().size() <= 3) {
                gameService.shutDownGame(gameRoomId);
            }

            // 해당 유저를 GameRoomUser 에서 삭제
            gameRoomUserRepository.deleteByWebSessionId(webSessionId);

            // 구독된 같은 방 사람들에게 퇴장 메세지 보내기
            ChatMessage chatMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.LEAVE)
                    .roomId(currentGameRoom.getId())
                    .userId(gameRoomUser.getUserId())
                    .nickname(gameRoomUser.getNickname())
                    .content(String.format("%s 님이 퇴장하셨습니다.", gameRoomUser.getNickname()))
                    .build();

            log.info(">>>>>>> 위치 : GameRoomService 의 exitGameRoom 메서드 / 메시지 타입 : {}", chatMessage.getType());
            log.info(">>>>>>> 위치 : GameRoomService 의 exitGameRoom 메서드 / 메시지 내용 : {}", chatMessage.getContent());
            log.info(">>>>>>> 위치 : GameRoomService 의 exitGameRoom 메서드 / 나가려는 방 : {}", currentGameRoom.getId());

            sendingOperations.convertAndSend("/topic/chat/room/" + currentGameRoom.getId(), chatMessage);

            // 유저가 나간 방의 UserList 정보 가져오기
            List<GameRoomUser> leftGameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(currentGameRoom.getId());

            // 남은인원 0명이면 GameRoom 삭제
            if (leftGameRoomUserList.size() == 0){
                gameRoomRepository.deleteById(gameRoomUser.getGameRoom().getId());
            }

            // 방장이 방을 나갔고, GameRoom 에 User 남아있을 경우
            if (gameRoomUser.getUserId().equals(currentGameRoom.getHostId()) && !leftGameRoomUserList.isEmpty()) {

                // 게임 방 유저들중 현재 방장 다음으로 들어온 UserId 가져오기
                GameRoomUser newHost = leftGameRoomUserList.get(0);

                // gameRoomUser 정보로 새로운 Host 의 id 와 nickname 가져오기
                Long hostId = newHost.getUserId();
                String hostNick = newHost.getNickname();

                // 새로운 Host 가 선정되어 id 와 nickname 을 업데이트
                currentGameRoom.GameRoomUpdate(hostId, hostNick);

                // 새로운 Host 의 readyStatus 를 true 로 변경
                newHost.update(true);
            }
        }
        // SSE event 생성
        sseEmitters.changeRoom(sseService.getRooms(-1));
    }

    // ======================== 유저별 웹세션 ID 업데이트 =========================
    @Transactional
    public void updateWebSessionId(Long gameRoomId, String token, String webSessionId) {
        // 받아온 토큰으로부터 유저 또는 guest 정보를 받아온다.
        HashMap<String, String> extInfo = userService.getGamerInfo(token);
        Long gamerId = Long.valueOf(extInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>>> userId {}", gamerId);
        log.info(">>>>>>>> gameRoomId {}", gameRoomId);

        // 해당 User 데이터로 GameRoomUser 데이터 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(gamerId, gameRoomId);

        // 해당 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 해당 GameRoomUser 에 WebSessionId 업데이트
        gameRoomUser.update(gameRoom.getHostId().equals(gamerId), webSessionId);
    }

    // =================== 본인을 제외한 GameRoom 의 유저 리스트 ===================
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllGameRoomUsersExceptMe(Long roomId, String webSessionId){
        // GameRoomId 데이터로 GameRoomUser 데이터의 List 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(roomId);

        // 해당 방에 본인을 제외한 전체 유저 리스트 생성
        // 예) [ { id : webSessionId1 }, { id : webSessionId2 }, ...  ]
        List<Map<String, String>> originGameRoomWebSessionIdList = new ArrayList<>();

        for (GameRoomUser gameRoomUser : gameRoomUserList){

            // GameRoomUser 의 WebSessionId 가져오기
            String userSessionId = gameRoomUser.getWebSessionId();

            // 만약 가져온 WebSessionId 와 본인( webSessionId ) 가 같지않다면
            if (!userSessionId.equals(webSessionId)) {
                // userMap 선언
                Map<String, String> userMap = new HashMap<>();

                // 본인을 제외한 GameRoomUser 의 WebSessionId 추가
                userMap.put("id", userSessionId);

                // userMap 을 List 의 객체로 추가
                originGameRoomWebSessionIdList.add(userMap);
            }
        }
        return originGameRoomWebSessionIdList;
    }

    // =============== 해당 GameRoom 의 전체 유저 session id 리스트 ===============
    @Transactional(readOnly = true)
    public List<String> getAllGameRoomUsersSessionId(Long roomId) {
        // 받아온 방 번호로 gameRoomUser 의 List 조회
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(roomId);

        // webSessionId만 꺼내서 새로운 List 로 만들어 반환
        List<String> allUsersSessionId = new ArrayList<>();
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            allUsersSessionId.add(gameRoomUser.getWebSessionId());
        }
        return allUsersSessionId;
    }

    // =============== 해당 GameRoom 접속 유저의 ready 상태 업데이트 ================
    @Transactional
    public boolean updateReadyStatus(Long gameRoomId, String webSessionId) {
        // 받아온 방 번호와 webSessionId 로 gameRoomUser 조회
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByGameRoomIdAndWebSessionId(gameRoomId, webSessionId);

        // 해당 유저의 readyStatus 가 true 이면 false 로, false 이면 true 로 변경하여 반환
        gameRoomUser.update(!gameRoomUser.isReadyStatus());
        return gameRoomUser.isReadyStatus();
    }

    // ====================== 본인을 포함한 현재 방의 전체 유저 ======================
    @Transactional
    public List<Map<String, Object>> getAllGameRoomUsers(Long gameRoomId) {
        // 해당 방의 정보를 가져와서 Host id 조회
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 해당 방 번호로 gameRoomUser 의 List 를 조회하여 원하는대로 가공
        List<Map<String, Object>> attendee = new ArrayList<>();
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoomId);
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            Map<String, Object> gameRoomUserMap = new HashMap<>();
            gameRoomUserMap.put("userId", gameRoomUser.getUserId().toString());
            gameRoomUserMap.put("nickname", gameRoomUser.getNickname());
            gameRoomUserMap.put("imgUrl", gameRoomUser.getImgUrl());
            gameRoomUserMap.put("isReady", gameRoomUser.isReadyStatus());
            gameRoomUserMap.put("socketId", gameRoomUser.getWebSessionId());
            gameRoomUserMap.put("isHost", gameRoomUser.getUserId().equals(gameRoom.getHostId()));
            attendee.add(gameRoomUserMap);
        }
        return attendee;

    }

    // ======================== 접속한 유저의 webSessionId  =======================
    @Transactional
    public Long getRoomId(String webSessionId) {
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(webSessionId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_ROOM_USER_NOT_FOUND)
        );
        return gameRoomUser.getGameRoom().getId();
    }

}
