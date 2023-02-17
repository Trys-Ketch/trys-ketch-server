package com.project.trysketch.global.rtc;

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
@Slf4j
@Component
public class SignalingHandler extends TextWebSocketHandler {

    // service 주입
    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GameService gameService;

    // 세션 정보 저장 -> { webSessionId1 : 세션객체, webSessionId2 : 세션객체, ... }
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 시그널링에 사용되는 메시지 타입 :
    // SDP Offer 메시지
    private static final String MSG_TYPE_OFFER = "rtc/offer";
    // SDP Answer 메시지
    private static final String MSG_TYPE_ANSWER = "rtc/answer";
    // 새로운 ICE Candidate 메시지
    private static final String MSG_TYPE_CANDIDATE = "rtc/candidate";
    // 본인을 제외한 현재방의 유저 리스트 반환 메시지
    private static final String MSG_TYPE_ALL_USERS = "rtc/all_users";
    // 방 나가기 메시지
    private static final String MSG_TYPE_USER_EXIT = "rtc/user_exit";

    // 방 입장 메시지
    private static final String MSG_TYPE_JOIN_ROOM = "ingame/join_room";
    // 게임 준비 메시지
    private static final String MSG_TYPE_TOGGLE_READY = "ingame/toggle_ready";
    // 현재 게임 방의 전체 유저 정보 메시지
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

            // 유저 uuid 와 roomId 와 token 을 저장
            String webSessionId = session.getId(); // 유저 uuid
            Long roomId = message.getRoom();   // roomId
            String token = message.getToken(); // 로그인한 유저 token
            log.info(">>> [ws] 메시지 타입 {}, 보낸 사람 {}, 방 번호 {}", message.getType(), webSessionId, roomId);

            // 메시지 타입에 따라서 서버에서 하는 역할이 달라진다
            switch (message.getType()) {

                // 방 입장
                case MSG_TYPE_JOIN_ROOM:


                    // 세션 저장, user 정보 저장 -> 방 입장
                    sessions.put(webSessionId, session);

                    // gameroomId 와 token 에 있는 userId 로 GameRoomRepository 에서 해당 gameRoomUser 데이터를 찾고
                    // 해당 webSessionId 로 접속한 sessionId를 update
                    gameRoomService.updateWebSessionId(roomId, token, webSessionId);


                    // 해당 방에 다른 유저가 있었다면 offer-answer 를 위해 유저 리스트를 만들어 클라이언트에 전달

                    // 본인을 제외한 해당 방의 다른 유저들
                    // 예) [ { id : webSessionId1 }, { id : webSessionId2 }, ...  ]

                    // rtc/all_users 라는 타입으로, 본인에게 메시지 전달
                    session.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_ALL_USERS)
                            .allUsers(gameRoomService.getAllGameRoomUsersExceptMe(roomId, webSessionId))
                            .sender(webSessionId).build())));

                    // 본인을 포함한 현재 방의 전체 유저 정보
                    // 예) [ { userId: 2, nickname: "닉네임", imgUrl: "avatar.png", isHost: true, isReady: true, socketId: "qw5lkvtn"}, { ... } ]

                    // ingame/attendee 라는 타입으로, 해당 방의 전체 유저에게 메시지 전달
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(webSessionId).build())));
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
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

                    // sessions 에서 receiver 를 찾아 메시지 전달
                    sessions.values().forEach(s -> {
                        try {
                            if(s.getId().equals(receiver)) {
                                s.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(message.getType())
                                        .sdp(sdp)
                                        .candidate(candidate)
                                        .sender(webSessionId)
                                        .receiver(receiver).build())));
                            }
                        } catch (Exception e) {
                            log.info(">>> [ws] 에러 발생 : offer," +
                                    " candidate, answer 메시지 전달 실패 {}", e.getMessage());
                        }
                    });
                    break;

                // 유저 게임 준비
                case MSG_TYPE_TOGGLE_READY:


                    // 접속한 유저의 roomId와 webSessionId 를 service 에 넘겨서 status 변경
                    // 해당 유저의 readyStatus 가 true 였다면 false 로, false 였다면 true 로 DB 업데이트
                    boolean userReadyStatus = gameRoomService.updateReadyStatus(roomId, webSessionId);

                    // 본인을 포함한 현재 방의 전체 유저 정보
                    // 예) [ { userId: 2, nickname: "닉네임", imgUrl: "avatar.png", isHost: true, isReady: true, socketId: "qw5lkvtn"}, { ... } ]
                    // ingame/attendee 라는 타입으로, 해당 방의 전체 유저에게 메시지 전달
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(webSessionId).build())));
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }
                    break;

                // 강퇴
                case MSG_TYPE_KICK:
                    // 방장의 강퇴 기능 구현

                    String kickId = message.getKickId();

                    // 강퇴 당하는 사람에게만 메시지 발송
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            // 방 안의 전체 각각의 유저별로 강퇴 당하는 사람 찾아서 메시지 발송
                            if (webSocketSession.getId().equals(kickId)) {
                                gameRoomService.exitGameRoom(kickId, roomId);

                                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(MSG_TYPE_BE_KICKED)
                                        .sender(kickId).build())));
                                break;
                            }
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 강퇴 당한 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }
                    // 방에 남아있는 유저에게만 메시지 발송
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            if (!webSocketSession.getId().equals(kickId)) {
                                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(MSG_TYPE_ATTENDEE)
                                        .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                        .sender(webSocketSession.getId()).build())));

                                webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                        .type(MSG_TYPE_USER_EXIT)
                                        .sender(webSocketSession.getId()).build())));
                            }
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }

                    break;

                // 게임 한 판 끝난 후
                case MSG_TYPE_ENDGAME:

                    // 본인을 포함한 현재 방의 전체 유저 정보
                    // 예) [ { userId: 2, nickname: "닉네임", imgUrl: "avatar.png", isHost: true, isReady: true, socketId: "qw5lkvtn"}, { ... } ]
                    // ingame/attendee 라는 타입으로, 해당 방의 전체 유저에게 메시지 전달
                    try {
                        for (WebSocketSession webSocketSession : getRoomSessionList(roomId)) {
                            webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                                    .type(MSG_TYPE_ATTENDEE)
                                    .attendee(gameRoomService.getAllGameRoomUsers(roomId))
                                    .sender(webSessionId).build())));
                        }
                    } catch (Exception e) {
                        log.info(">>> [ws] 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
                    }

                    break;

                // 메시지 타입이 잘못 되었을 경우
                default:
                    log.info(">>> [ws] 잘못된 메시지 타입 {}", message.getType());
            }
        } catch (IOException e) {
            log.info(">>> [ws] 에러 발생 : 양방향 데이터 통신 실패 {}", e.getMessage());
        }
    }


    // 소켓 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info(">>> [ws] 클라이언트 접속 해제 : 세션 - {}, 상태 - {}", session, status);

        // 유저 uuid 와 roomID 를 저장
        String webSessionId = session.getId();                     // 유저 uuid
        Long gameRoomId = gameRoomService.getRoomId(webSessionId); // roomId

        gameService.submitLeftRound(webSessionId);
        gameRoomService.exitGameRoom(webSessionId, gameRoomId);

        // 연결이 종료되면 sessions 와 userInfo 에서 해당 유저 삭제
        sessions.remove(webSessionId);

        log.info(">>> [ws] {}를 제외한 남은 세션 객체 {}", webSessionId, sessions);

        // 본인을 제외한 현재 방의 전체 유저 정보
        // 강퇴기능으로 인해 만약 받아온 webSessionId 가 null 이 아닌 그냥 유저가 방을 나가거나 연결이 끊긴 경우 시작
        // 예) [ { userId: 2, nickname: "닉네임", imgUrl: "avatar.png", isHost: true, isReady: true, socketId: "qw5lkvtn"}, { ... } ]
        // ingame/attendee & rtc/user_exit 라는 타입으로, 해당 방의 전체 유저에게 메시지 전달
        if (gameRoomId != null) {
            try {
                for (WebSocketSession webSocketSession : getRoomSessionList(gameRoomId)) {
                    webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_ATTENDEE)
                            .attendee(gameRoomService.getAllGameRoomUsers(gameRoomId))
                            .sender(webSessionId).build())));
                    webSocketSession.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_USER_EXIT)
                            .sender(webSessionId).build())));
                }
            } catch (Exception e) {
                log.info(">>> [ws] 에러 발생 : 해당 방 전체 유저에게 메시지 전달 실패 {}", e.getMessage());
            }
        }

    }


    // 소켓 통신 에러
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.info(">>> [ws] 에러 발생 : 소켓 통신 에러 {}", exception.getMessage());
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
                log.info(">>> [ws] 에러 발생 : 본인을 포함한 현재 방의 세션 객체 리스트 생성 실패 {}", e.getMessage());
            }
        });
        return sessionList;
    }
}