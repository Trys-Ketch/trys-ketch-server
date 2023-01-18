package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.dto.response.GameRoomResponseDto;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.redis.dto.GamerEnum;
import com.project.trysketch.repository.GameRoomRepository;
import com.project.trysketch.repository.GameRoomUserRepository;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.redis.entity.Guest;
import com.project.trysketch.redis.repositorty.GuestRepository;
import com.project.trysketch.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final GuestRepository guestRepository;
    private final UserService userService;
    private final SseEmitters sseEmitters;

    // ============================== 게임방 조회 ==============================
    @Transactional
    public Map<String, Object> getAllGameRoom(Pageable pageable) {

        // pageable 객체로 GameRoom 을 Page<> 에 담아 가져오기
        Page<GameRoom> rooms = gameRoomRepository.findAll(pageable);

        // GameRoom 을 Dto 형태로 담아줄 List 선언
        List<GameRoomResponseDto> gameRoomList = new ArrayList<>();

        // GameRoom 의 정보와 LastPage 정보를 담아줄 Map 선언
        Map<String, Object> getAllGameRoom = new HashMap<>();

        for (GameRoom gameRoom : rooms){

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
        getAllGameRoom.put("LastPage",rooms.getTotalPages());

        return getAllGameRoom;
    }

    // ============================== 게임방 생성 ==============================
    @Transactional
    public DataMsgResponseDto createGameRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request) {
        // 1. 받아온 헤더로부터 유저 또는 guest 정보를 받아온다.
        String header = userService.validHeader(request);
        HashMap<String, String> extInfo = userService.gamerInfo(header);

        // 2. 요청을 한 유저가 이미 속한 방이 있으면 생성 불가능
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))) {
            throw new CustomException(StatusMsgCode.ONE_MAN_ONE_ROOM);
        }

        // 3. 초대 코드를 위한 랜덤코드 생성
        String randomCode = "";
        StringBuffer key = new StringBuffer();
        Random rnd = new Random();

        // 소문자와 숫자 8자리 조합 만들기
        for (int i = 0; i < 8; i++) {
            int index = rnd.nextInt(2); // 0~1 중 랜덤

            switch (index) {
                case 0:
                    //  a~z  (ex. 1+97=98 => (char)98 = 'b')
                    key.append((char) ((rnd.nextInt(26)) + 97));
                    break;
                case 1:
                    // 0~9
                    key.append((rnd.nextInt(10)));
                    break;
            }
        }
        randomCode = key.toString();

        // 4. 방 정보 생성
        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomRequestDto.getTitle())
                .hostId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))
                .hostNick(extInfo.get(GamerEnum.NICK.key()))
                .isPlaying(false)
                .randomCode(randomCode)
                .build();

        // 4. 방에 입장한 유저 정보 생성
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

        // 5. 게임 방 DB에 저장 및 입장중인 유저 정보 저장
        gameRoomRepository.save(gameRoom);
        gameRoomUserRepository.save(gameRoomUser);

        HashMap<String, String> roomInfo = new HashMap<>();

        // 7. HashMap 형식으로 방 제목, 방 번호, 방 랜덤코드를 response 로 반환
        roomInfo.put("gameRoomTitle",gameRoom.getTitle());
        roomInfo.put("roomId", String.valueOf(gameRoom.getId()));
        roomInfo.put("randomCode", randomCode);

        // 8. SSE event 생성
        sseEmitters.changeRoom(getrooms());

        return new DataMsgResponseDto(StatusMsgCode.OK,roomInfo);
    };


    // ============================== 초대 게임방 입장 ==============================
    @Transactional
    public DataMsgResponseDto enterGameRoom(String randomCode, HttpServletRequest request) {
        // 1. 받아온 헤더로부터 유저 또는 guest 정보를 받아온다.
        String header = userService.validHeader(request);
        HashMap<String, String> extInfo = userService.gamerInfo(header);

        // 2. id로 DB 에서 현재 들어갈 게임방 데이터 찾기
//        Optional<GameRoom> enterGameRoom = gameRoomRepository.findById(id);
        GameRoom enterGameRoom = gameRoomRepository.findByRandomCode(randomCode).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 3. 게임 방의 상태가 true 이면 게임이 시작중이니 입장 불가능
        if (enterGameRoom.isPlaying()){
            return new DataMsgResponseDto(StatusMsgCode.ALREADY_PLAYING);
        }

        // 4. 현재 방의 인원이 8명 이상이면 풀방임~
        Long checkUsers = gameRoomUserRepository.countByGameRoomIdOrderByUserId(enterGameRoom.getId());
        if (checkUsers >= 8) {
            return new DataMsgResponseDto(StatusMsgCode.FULL_BANG);
        }

        // 5. 현재 User 가 다른 방에 들어가 있다면
        if (gameRoomUserRepository.existsByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))) {
            return new DataMsgResponseDto(StatusMsgCode.ONE_MAN_ONE_ROOM);
        }

        // 6. 새롭게 게임방에 들어온 유저 생성
        GameRoomUser gameRoomUser = GameRoomUser.builder()
                .gameRoom(enterGameRoom)
                .userId(Long.valueOf(extInfo.get(GamerEnum.ID.key())))
                .nickname(extInfo.get(GamerEnum.NICK.key()))
                .imgUrl(extInfo.get(GamerEnum.IMG.key()))
                .webSessionId(null)
                .readyStatus(false)
                .build();

        // 7. 게임방에 들어온 유저를 DB에 저장
        gameRoomUserRepository.save(gameRoomUser);


        HashMap<String, Long> roomIdInfo = new HashMap<>();
        roomIdInfo.put("roomId", enterGameRoom.getId());
        return new DataMsgResponseDto(StatusMsgCode.OK, roomIdInfo);
    }

    // ============================= 게임방 나가기 =============================
    @Transactional
    public MsgResponseDto exitGameRoom(Long id, HttpServletRequest request, String userUUID) {
        HashMap<String, String> extInfo = new HashMap<>();

        // 1. 분기처리 시작
        if (request != null && userUUID == null) {
            // 2. API 요청으로 게임방을 나가는 경우
            String header = userService.validHeader(request);
            extInfo = userService.gamerInfo(header);
        } else {
            // 3. 소켓 연결이 종료됨으로 인해서 게임방에서 나가지는 경우
            // 예) 웹 브라우저 창 닫기, 팅겼을 경우
            GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(userUUID);
            extInfo.put(GamerEnum.ID.key(), gameRoomUser.getUserId().toString());
            extInfo.put(GamerEnum.NICK.key(), gameRoomUser.getNickname());
            id = gameRoomUser.getGameRoom().getId();
        }

        // 4. 유저가 나가려고 하는 GameRoom(방) 정보 가져오기
        GameRoom enterGameRoom = gameRoomRepository.findById(id).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 5. 나가려고 하는 GameRoomUser(유저) 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserId(Long.valueOf(extInfo.get(GamerEnum.ID.key())));

        // 6. 나가려는 유저가 요청한 방에 존재하지 않으면 잘못된 요청
        if (!gameRoomUserRepository.existsByGameRoomIdAndUserId(id, gameRoomUser.getUserId())) {
            return new MsgResponseDto(StatusMsgCode.ONE_MAN_ONE_ROOM);
        }

        // 7. 해당 유저 를 GameRoomUser 에서 삭제
        gameRoomUserRepository.delete(gameRoomUser);

        // 8. 유저가 나간 방의 UserList 정보 가져오기
        List<GameRoomUser> leftGameRoomUserList = gameRoomUserRepository.findAllByGameRoom(enterGameRoom);

        // 9. 게임 방의 남은 인원이 0명이면 게임 방도 삭제
        if (leftGameRoomUserList.size() == 0){
            gameRoomRepository.delete(enterGameRoom);
        }

        // 10. 나간 User 와 해당 GameRoom 의 방장이 같으며 GameRoom 에 User 남아있을 경우
        if (Long.valueOf(extInfo.get(GamerEnum.ID.key())).equals(enterGameRoom.getHostId()) && !leftGameRoomUserList.isEmpty()) {
            Long hostId = null;
            String hostNick = null;

            // 11. 게임 방 유저들중 현재 방장 다음으로 들어온 UserId 가져오기
            Long newHostId = leftGameRoomUserList.get(0).getUserId();

            // 12. UserId 를 들고 GameRoomUser 정보 가져오기
            GameRoomUser userHost = gameRoomUserRepository.findByUserId(newHostId);
            Guest guestHost = guestRepository.findById(String.valueOf(newHostId)).orElse(null);

            // 13. null 값 여부로 회원, 비회원 판단후 host 에 닉네임 넣기
            if (userHost != null) {
                hostId = userHost.getUserId();
                hostNick = userHost.getNickname();
            } else if (guestHost != null) {
                hostId = Long.valueOf(guestHost.getGuestId());
                hostNick = guestHost.getNickname();
            }

            // 14. 새로운 Host 가 선정되어 GameRoom 정보 빌드
            GameRoom updateGameRoom = GameRoom.builder()
                    .id(enterGameRoom.getId())
                    .hostId(hostId)
                    .hostNick(hostNick)
                    .title(enterGameRoom.getTitle())
                    .isPlaying(false)
                    .build();

            // 15. 기존 GameRoom 에 새로 빌드된 GameRoom 정보 업데이트
            gameRoomRepository.save(updateGameRoom);

            // 16. SSE event 생성
            sseEmitters.changeRoom(getrooms());
        }
        return new MsgResponseDto(StatusMsgCode.SUCCESS_EXIT_GAME);
    }

    // ======================== 유저별 웹세션 ID 업데이트 ========================
    @Transactional
    public void updateWebSessionId(Long gameRoomId, String token, String userUUID) {
        // 1. 받아온 토큰으로부터 유저 또는 guest 정보를 받아온다.
        HashMap<String, String> extInfo = userService.gamerInfo(token);
        Long gamerId = Long.valueOf(extInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>>> gamerId {}", gamerId);
        log.info(">>>>>>>> gameRoomId {}", gameRoomId);

        // 2. 해당 User 데이터로 GameRoomUser 데이터 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(gamerId, gameRoomId);

        // 3. 해당 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        // 4. 해당 GameRoomUser 에 WebSessionId 업데이트
        if (gameRoom.getHostId().equals(gamerId)) {
            gameRoomUser.update(true, userUUID);
        } else {
            gameRoomUser.update(false, userUUID);
        }
    }

    // =================== 본인을 제외한 GameRoom 의 유저 리스트 ===================
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllGameRoomUsersExceptMe(Long roomId, String userUUID){

        // 1. GameRoomId 데이터로 GameRoomUser 데이터의 List 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(roomId);

        // 2. 해당 방에 본인을 제외한 전체 유저 리스트 생성
        // 예) [ { id : userUUID1 }, { id : userUUID2 }, ...  ]
        List<Map<String, String>> originGameRoomUserUUIDList = new ArrayList<>();

        for (GameRoomUser gameRoomUser : gameRoomUserList){

            // 3. GameRoomUser 의 WebSessionId 가져오기
            String userSessionId = gameRoomUser.getWebSessionId();

            // 4. 만약 가져온 WebSessionId 와 본인( userUUID ) 가 같지않다면
            if (!userSessionId.equals(userUUID)) {
                // 5. userMap 선언
                Map<String, String> userMap = new HashMap<>();

                // 6. 본인을 제외한 GameRoomUser 의 WebSessionId 추가
                userMap.put("id", userSessionId);

                // 7. userMap 을 List 의 객체로 추가
                originGameRoomUserUUIDList.add(userMap);
            }
        }
        return originGameRoomUserUUIDList;
    }

    // ================= 해당 GameRoom 의 전체 유저 session id 리스트 =================
    @Transactional(readOnly = true)
    public List<String> getAllGameRoomUsers(Long roomId) {

        // 1. 받아온 방 번호로 gameRoomUser 의 List 조회
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(roomId);

        // 2. webSessionId만 꺼내서 새로운 List 로 만들어 반환
        List<String> allUsersSessionId = new ArrayList<>();
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            allUsersSessionId.add(gameRoomUser.getWebSessionId());
        }
        return allUsersSessionId;
    }

    // =================== 해당 GameRoom 접속 유저의 ready 상태 업데이트 ===================
    @Transactional
    public boolean updateReadyStatus(Long gameRoomId, String userUUID) {

        // 1. 받아온 방 번호와 userUUID 로 gameRoomUser 조회
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByGameRoomIdAndWebSessionId(gameRoomId, userUUID);

        // 2. 해당 유저의 readyStatus 가 true 이면 false 로, false 이면 true 로 변경하여 반환
        if (gameRoomUser.isReadyStatus()) {
            gameRoomUser.update(false);
            return false;
        } else {
            gameRoomUser.update(true);
            return true;
        }

    }

    // ============ 해당 GameRoom 전체 유저의 ready 상태와 방장 webSessionId 조회 ============
    @Transactional(readOnly = true)
    public Map<String, Object> getGameReadyStatus(Long gameRoomId) {

        // 1. 결과를 담을 HashMap 선언
        Map<String, Object> gameReadyStatusAndHost = new HashMap<>();

        // 2. 현재 gameRoom 방장의 유저 id 추출
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        Long hostId = gameRoom.getHostId();

        // 3. 해당 gameRoom 모든 유저의 ready 상태가 true && 방 인원이 4명 이상이면 flag = true
        boolean flag = true;
        String hostWebSessionId = "";
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoomId);
        if (gameRoomUserList.size() < 2) {        // FIXME
            flag = false;                         // 현재 게임 시작 최소 인원 2명으로 테스트 중
        }
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            if (!gameRoomUser.isReadyStatus()) {
                flag = false;
            }
            // 4. 위에서 가져온 방장의 유저 id로 방장의 webSessionId 추출
            if (hostId.equals(gameRoomUser.getUserId())) {
                hostWebSessionId = gameRoomUser.getWebSessionId();
            }
        }

        // 모든 유저의 ready 상태가 true 라면 gameRoom status true 로 변경 -> 게임 시작 부분으로 이동
//        if (flag) {
//            gameRoom.GameRoomStatusUpdate(true);
//        }

        // 5. 게임 가능 상태와 방장의 webSessionId 반환
        gameReadyStatusAndHost.put("status", flag);
        gameReadyStatusAndHost.put("host", hostWebSessionId);
        return gameReadyStatusAndHost;

    }

    // ===================== 해당 방의 방장 여부와 방장 session id 조회 =====================
    @Transactional
    public Map<String, Object> getGameRoomHost(Long gameRoomId, String userUUID) {

        // 1. 결과를 담을 HashMap 선언
        Map<String, Object> hostMap = new HashMap<>();

        // 2. 현재 gameRoom 의 방장 유저 id 추출
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        Long hostId = gameRoom.getHostId();

        String hostWebSessionId = "";
        boolean amIHost = false;

        // 3. 받아온 roomId 로 gamRoomUserList 를 조회한 후,
        //    방장의 webSessionId 와 접속한 유저의 방장 여부를 반환
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoomId);
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            if (gameRoomUser.getUserId().equals(hostId)) {
                hostWebSessionId = gameRoomUser.getWebSessionId();
            }
            if (gameRoomUser.getUserId().equals(hostId) && gameRoomUser.getWebSessionId().equals(userUUID)) {
                amIHost = true;
            }
        }
        hostMap.put("isHost", amIHost);
        hostMap.put("hostId", hostWebSessionId);
        return hostMap;
    }



    // ===================== 모든 gameRoom 호출하는 메서드 ===============================
    public List<GameRoomResponseDto> getrooms(){
        // gameRoomResponseDto list 생성
        List<GameRoomResponseDto> gameRoomList = new ArrayList<>();

        // 모든 gameRoom 불러와서 dto에 담기 -> list에 저장
        List<GameRoom> gameRooms = gameRoomRepository.findAll();
        for (GameRoom room : gameRooms){

            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(room.getId())
                    .title(room.getTitle())
                    .hostNick(room.getHostNick())
                    .GameRoomUserCount(room.getGameRoomUserList().size())
                    .isPlaying(room.isPlaying())
                    .createdAt(room.getCreatedAt())
                    .modifiedAt(room.getModifiedAt())
                    .randomCode(room.getRandomCode())
                    .build();
            gameRoomList.add(gameRoomResponseDto);
        }
        return gameRoomList;
    }
}
