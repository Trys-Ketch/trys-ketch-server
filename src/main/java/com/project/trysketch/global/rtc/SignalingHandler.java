package com.project.trysketch.global.rtc;

import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.service.GameRoomService;
import com.project.trysketch.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 1. 기능   : Signaling Server 역할
// 2. 작성자 : 안은솔
// 3. 참고사항: 추후 게임방 DB와 연결 필요
@Slf4j
@Component
public class SignalingHandler extends TextWebSocketHandler {

    // service 주입
    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GameService gameService;

    // 어떤 방에 어떤 유저가 들어있는지 저장 -> { 방번호 : [ { id : userUUID1 }, { id: userUUID2 }, …], ... }
    private final Map<String, List<Map<String, String>>> roomInfo = new HashMap<>();

    // userUUID 기준 어떤 방에 들어있는지 저장 -> { userUUID1 : 방번호, userUUID2 : 방번호, ... }
    private final Map<String, String> userInfo = new HashMap<>();

    // 세션 정보 저장 -> { userUUID1 : 세션객체, userUUID2 : 세션객체, ... }
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 방의 최대 인원수
    private static final int MAXIMUM = 8;


    // 시그널링에 사용되는 메시지 타입 :
    // SDP Offer 메시지
    private static final String MSG_TYPE_OFFER = "rtc/offer";
    // SDP Answer 메시지
    private static final String MSG_TYPE_ANSWER = "rtc/answer";
    // 새로운 ICE Candidate 메시지
    private static final String MSG_TYPE_CANDIDATE = "rtc/candidate";
    // 방 입장 메시지
    private static final String MSG_TYPE_JOIN_ROOM = "ingame/join_room";
    // 본인을 제외한 현재방의 유저 리스트 반환 메시지
    private static final String MSG_TYPE_ALL_USERS = "rtc/all_users";
    // 방 나가기 메시지
    private static final String MSG_TYPE_USER_EXIT = "rtc/user_exit";
    // 게임 준비 메시지
    private static final String MSG_TYPE_TOGGLE_READY = "ingame/toggle_ready";
    // 접속한 유저의 준비 상태 반환 메시지
    private static final String MSG_TYPE_READY = "ingame/ready";
    // 해당 방 전체 유저의 준비 상태 반환 메시지
    private static final String MSG_TYPE_ALL_READY = "ingame/all_ready";
    // 방장 여부와 방장 session id 반환 메시지
    private static final String MSG_TYPE_IS_HOST = "ingame/is_host";
    private static final String MSG_TYPE_ATTENDEE = "ingame/attendee";
    // 게임이 끝났을 때 반환 메시지
    private static final String MSG_TYPE_ENDGAME = "ingame/end_game";
    // 강퇴시 반환 메시지
    private static final String MSG_TYPE_KICK = "ingame/kick";
    // 강퇴당한 사람 반환 메시지
    private static final String MSG_TYPE_BE_KICKED = "ingame/be_kicked";


    // 웹소켓 연결 시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info(">>> [ws] 클라이언트 접속 : 세션 - {}", session);
    }

    // 양방향 데이터 통신
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        try {
            // 웹 소켓으로부터 전달받은 메시지를 deserialization(JSON -> Java Object)
            Message message = Utils.getObject(textMessage.getPayload());
            log.info(">>> [ws] 시작!!! 세션 객체 {}", session);

            // 유저 uuid 와 roomId 와 token 을 저장
            String userUUID = session.getId(); // 유저 uuid
            Long roomId = message.getRoom();   // roomId
            String token = message.getToken(); // 로그인한 유저 token
            log.info(">>> [ws] 메시지 타입 {}, 보낸 사람 {}, 방 번호 {}", message.getType(), userUUID, roomId);

            // 메시지 타입에 따라서 서버에서 하는 역할이 달라진다
            switch (message.getType()) {

                // 방 입장
                case MSG_TYPE_JOIN_ROOM:

                    log.info(">>> [ws] {} 가 #{}번 방에 들어감", userUUID, roomId);

                    // 세션 저장, user 정보 저장 -> 방 입장
                    sessions.put(userUUID, session);

                    // gameroomId 와 token 에 있는 userId 로 GameRoomRepository 에서 해당 gameRoomUser 데이터를 찾고
                    // 해당 webSessionId 로 접속한 sessionId를 update
                    gameRoomService.updateWebSessionId(roomId, token, userUUID);

                    // 방장 여부와 해당 방의 방장 session id 가져오기
                    Map<String, Object> gameRoomHost = gameRoomService.getGameRoomHost(roomId, userUUID);
                    log.info(">>> [ws] {} 가 방장인가? {}", userUUID, gameRoomHost.get("isHost"));
                    log.info(">>> [ws] #{}번 방 방장은 누구인가? {}", roomId, gameRoomHost.get("hostId"));


                    // 해당 방에 다른 유저가 있었다면 offer-answer 를 위해 유저 리스트를 만들어 클라이언트에 전달

                    // roomInfo = { 방번호 : [ { id : userUUID1 }, { id: userUUID2 }, …], 방번호 : [ { id : userUUID3 }, { id: userUUID4 }, …], ... }
                    // originRoomUser -> 본인을 제외한 해당 방의 다른 유저들
                    List<Map<String, String>> originRoomUser = gameRoomService.getAllGameRoomUsersExceptMe(roomId, userUUID);
                    log.info(">>> [ws] 본인 {} 을 제외한 #{}번 방의 다른 유저들 {}", userUUID, roomId, originRoomUser);

                    // all_users 라는 타입으로 메시지 전달
                    session.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_ALL_USERS)
                            .allUsers(originRoomUser)
                            .sender(userUUID).build())));

                    // is_host 라는 타입으로 메시지 전달
                    session.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_IS_HOST)
                            .hostId((String) gameRoomHost.get("hostId"))
                            .host((boolean) gameRoomHost.get("isHost"))
                            .build())));

                    // TODO
                    // 본인을 포함한 현재 방의 전체 유저
                    // [ { userId: 2, nickname: "닉네임", imgUrl: "avatar.png", isHost: true, isReady: true }, { ... } ]

                    // attendee 라는 타입으로 메시지 전달
                    log.info(">>> [ws] 본인 {} 을 포함한 #{}번 방의 다른 유저들 {}", userUUID, roomId, gameRoomService.getAllGameRoomUsers(roomId));

                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            log.info(">>> [ws] #{}번 방에 있는 전체 유저의 세션 객체 리스트 {}", roomId, webSocketSession);
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(userUUID).build())));
                        }
                        log.info(">>> [ws] 본인의 정보를 해당 방 전체 유저에게 전달 성공!!");
                    } catch (Exception e) {
                        log.info(">>> 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }
                    break;

                // 클라이언트에게서 받은 메시지 타입에 따른 signal 프로세스
                case MSG_TYPE_OFFER:
                case MSG_TYPE_ANSWER:
                case MSG_TYPE_CANDIDATE:

                    // 전달받은 메시지로부터 candidate, sdp, receiver 를 저장
                    Object candidate = message.getCandidate();
                    Object sdp = message.getSdp();
                    String receiver = message.getReceiver();   // 클라이언트에서 보내주는 1명의 receiver
                    log.info(">>> [ws] receiver {}", receiver);

                    // sessions 에서 receiver 를 찾아 메시지 전달
                    sessions.values().forEach(s -> {
                        try {
                            if(s.getId().equals(receiver)) {
                                s.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(message.getType())
                                        .sdp(sdp)
                                        .candidate(candidate)
                                        .sender(userUUID)
                                        .receiver(receiver).build())));
                            }
                        } catch (Exception e) {
                            log.info(">>> 에러 발생 : offer," +
                                    " candidate, answer 메시지 전달 실패 {}", e.getMessage());
                        }
                    });
                    break;

                // 유저 게임 준비
                case MSG_TYPE_TOGGLE_READY:
                    // TODO
                    // 1. 접속한 유저의 roomId와 userUUID 를 service 에 넘겨서 status 변경
                    // 2. 나를 포함한 현재 방의 전체 유저 session 객체에게 접속한 유저의 변경된 status 메시지 보내기
                    // 3. 해당 방에 있는 모든 유저의 ready 상태가 true 이고,
                    //    방 인원이 2명 이상 이면 host 에게 게임 시작 가능 메시지 보내기

                    log.info(">>> [ws] #{}번 방 유저, {} 타입으로 들어옴", roomId, message.getType());

                    // 해당 유저의 readyStatus 가 true 였다면 false 로, false 였다면 true 로 DB 업데이트
                    boolean userReadyStatus = gameRoomService.updateReadyStatus(roomId, userUUID);
                    log.info(">>> [ws] #{}번 방에 있는 {}, 게임 준비 상태 {} ", roomId, userUUID, userReadyStatus);

                    // 본인을 포함한 현재 방의 전체 유저 객체를 가져와서 변경된 본인 ready 상태 메시지 전달
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            log.info(">>> [ws] #{}번 방에 있는 전체 유저의 세션 객체 리스트 {}", roomId, webSocketSession);
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_READY)
                                    .status(userReadyStatus)
                                    .sender(userUUID).build())));
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(userUUID).build())));
                        }
                        log.info(">>> [ws] 본인의 ready 변경 상태를 해당 방 전체 유저에게 전달 성공!!");
                    } catch (Exception e) {
                        log.info(">>> 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }

                    // 현재 방에 있는 모든 유저의 ready 상태가 true 인지 && 방 인원이 4명 이상인지 판단
                    Map<String, Object> gameReadyStatus = gameRoomService.getGameReadyStatus(roomId);
                    log.info(">>> [ws] #{}번 방 게임 시작 가능 여부 {}", roomId, gameReadyStatus.get("status"));
                    log.info(">>> [ws] #{}번 방 host 웹세션 id {}", roomId, gameReadyStatus.get("host"));

                    // 현재 방의 host 에게 게임 시작이 가능한 상태인지 메시지 전달
                    sessions.values().forEach(s -> {
                        if (gameReadyStatus.get("host").equals(s.getId())) {
                            try {
                                log.info(">>> [ws] #{}번 방의 host 웹세션 id {}", roomId, gameReadyStatus.get("host"));
                                s.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(MSG_TYPE_ALL_READY)
                                        .status((boolean) gameReadyStatus.get("status"))
                                        .build())));
                                log.info(">>> [ws] 현재 방이 게임을 시작할 수 있는 상태인지 방장에게 전달 성공!!");
                            }
                            catch (Exception e) {
                                log.info(">>> 에러 발생 : 방장에게 메시지 전달 실패 {}", e.getMessage());
                            }
                        }
                    });

                    break;

                case MSG_TYPE_KICK:
                    // 방장의 강퇴 기능 구현
                    log.info(">>> [ws] 강퇴 요청이 들어온 방의 번호 : #{}번 방", roomId);
                    log.info(">>> [ws] 방장이 강퇴 요청 / 강퇴 요청한 방장의 UUID : {}", userUUID);

                    String kickId = message.getKickId();
                    log.info(">>> [ws] 방장이 강퇴 요청 / 강퇴 당하는 유저의 UUID : {}", kickId);

                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            // 방 안의 전체 각각의 유저별로 강퇴 당하는 사람 찾아서 메시지 발송
                            if (webSocketSession.getId().equals(kickId)) {
                                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(MSG_TYPE_BE_KICKED)
                                        .sender(kickId).build())));
                                log.info(">>> [ws] 강퇴 당한 사람에게 메시지 전달 성공");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 강퇴 당한 사람에게 메시지 전달 실패 {}", e.getMessage());
                    }

                    break;

                case MSG_TYPE_ENDGAME:
                    // 본인을 포함한 현재 방의 전체 유저 객체를 가져와서 attendee, host 메시지 전달
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            log.info(">>> [ws] #{}번 방에 있는 전체 유저의 세션 객체 리스트 {}", roomId, webSocketSession);
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(userUUID).build())));

                            // 방 안의 전체 각각의 유저별로 방장 찾아서 메시지 발송
                            Map<String, Object> hostCheck = gameRoomService.getGameRoomHost(roomId, webSocketSession.getId());
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_IS_HOST)
                                    .hostId((String) hostCheck.get("hostId"))
                                    .host((boolean) hostCheck.get("isHost"))
                                    .build())));
                        }
                        log.info(">>> [ws] 현재 방 전체 사람들에게 상태 전송 성공 !!!");
                    } catch (Exception e) {
                        log.info(">>> 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }

                    break;

                // 메시지 타입이 잘못되었을 경우
                default:
                    log.info(">>> [ws] 잘못된 메시지 타입 {}", message.getType());
            }
        } catch (IOException e) {
            log.info(">>> 에러 발생 : 양방향 데이터 통신 실패 {}", e.getMessage());
        }
    }

    // 소켓 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info(">>> [ws] 클라이언트 접속 해제 : 세션 - {}, 상태 - {}", session, status);

        // 유저 uuid 와 roomID 를 저장
        String userUUID = session.getId(); // 유저 uuid
        Long gameRoomId = gameRoomService.getRoomId(userUUID);

        gameService.submitLeftRound(userUUID);
        gameRoomService.exitGameRoom(userUUID);

        // 연결이 종료되면 sessions 와 userInfo 에서 해당 유저 삭제
        sessions.remove(userUUID);

        log.info(">>> [ws] {}를 제외한 남은 세션 객체 {}", userUUID, sessions);

        // 본인을 제외한 모든 유저에게 user_exit 라는 타입으로 메시지 전달
        // FIXME
        // 모든 유저 아니고 현재 방 유저에게 보내야함

        // 본인을 제외한 현재 방의 전체 유저 객체를 가져와서 user_exit, attendee 메시지 전달
        try {
            for (WebSocketSession webSocketSession : getRoomSessionList(gameRoomId)) {
                log.info(">>> [ws] #{}번 방에 있는 전체 유저의 세션 객체 리스트 {}", gameRoomId, webSocketSession);
                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                        .type(MSG_TYPE_ATTENDEE)
                        .attendee(gameRoomService.getAllGameRoomUsers(gameRoomId))
                        .sender(userUUID).build())));

                // 방 안의 전체 각각의 유저별로 방장 찾아서 메시지 발송
                Map<String, Object> hostCheck = gameRoomService.getGameRoomHost(gameRoomId, webSocketSession.getId());
                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                        .type(MSG_TYPE_IS_HOST)
                        .hostId((String) hostCheck.get("hostId"))
                        .host((boolean) hostCheck.get("isHost"))
                        .build())));

                log.info(">>> [ws] user_exit 메시지 받고 있는 userUUID 리스트 {}", webSocketSession.getId());
                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                        .type(MSG_TYPE_USER_EXIT)
                        .sender(userUUID).build())));
            }
            log.info(">>> [ws] 본인의 방 나가기 상태를 해당 방 전체 유저에게 전달 성공!!");
        } catch (Exception e) {
            log.info(">>> 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
        }

//        sessions.values().forEach(s -> {
//            try {
//                if(!(s.getId().equals(userUUID))) {
//                    log.info(">>> [ws] user_exit 메시지 받고 있는 userUUID 리스트 {}", s.getId());
//                    s.sendMessage(new TextMessage(Utils.getString(Message.builder()
//                            .type(MSG_TYPE_USER_EXIT)
//                            .sender(userUUID).build())));
//                }
//            }
//            catch (Exception e) {
//                log.info(">>> 에러 발생 : user_exit 메시지 전달 실패 {}", e.getMessage());
//            }
//
//        });

    }


    // 소켓 통신 에러
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.info(">>> 에러 발생 : 소켓 통신 에러 {}", exception.getMessage());
    }

    // 본인을 포함한 현재 방의 세션 객체 리스트 반환
    public List<WebSocketSession> getRoomSessionList(Long roomId) {
        List<WebSocketSession> sessionList = new ArrayList<>();
        List<String> allUsers = gameRoomService.getAllGameRoomUsersSessionId(roomId);
        sessions.values().forEach(s -> {
            try {
                for (String allUser : allUsers) {
                    if (allUser.equals(s.getId())) {
                        sessionList.add(s);
                    }
                }
            } catch (Exception e) {
                log.info(">>> 에러 발생 : 본인을 포함한 현재 방의 세션 객체 리스트 생성 실패 {}", e.getMessage());
            }

        });
        return sessionList;
    }
}
