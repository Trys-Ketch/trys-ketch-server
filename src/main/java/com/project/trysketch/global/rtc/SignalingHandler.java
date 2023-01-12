package com.project.trysketch.global.rtc;

import com.project.trysketch.service.GameRoomService;
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
    private static final String MSG_TYPE_OFFER = "offer";
    // SDP Answer 메시지
    private static final String MSG_TYPE_ANSWER = "answer";
    // 새로운 ICE Candidate 메시지
    private static final String MSG_TYPE_CANDIDATE = "candidate";
    // 방 입장 메시지
    private static final String MSG_TYPE_JOIN = "join_room";
    // 본인을 제외한 현재방의 유저 리스트 반환 메시지
    private static final String MSG_TYPE_ALL_USERS = "all_users";
    // 방 나가기 메시지
    private static final String MSG_TYPE_EXIT = "user_exit";


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
            log.info(">>> [ws] 메시지 타입 {}, 보낸 사람 {}", message.getType(), userUUID);

            // 메시지 타입에 따라서 서버에서 하는 역할이 달라진다
            switch (message.getType()) {

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
                        }
                        catch (Exception e) {
                            log.info(">>> 에러 발생 : offer," +
                                    " candidate, answer 메시지 전달 실패 {}", e.getMessage());
                        }
                    });
                    break;

                // 방 입장
                case MSG_TYPE_JOIN:

                    log.info(">>> [ws] {} 가 #{}번 방에 들어감", userUUID, roomId);

                    // 방이 기존에 생성되어 있다면
//                    if (roomInfo.containsKey(roomId)) {
//
//                        // 현재 입장하려는 방에 있는 인원수
//                        int currentRoomLength = roomInfo.get(roomId).size();
//
//                        // 인원수가 꽉 찼다면 돌아간다
//                        if (currentRoomLength == MAXIMUM) {
//
//                            // 해당 유저에게 방이 꽉 찼다는 메시지를 보내준다
//                            session.sendMessage(new TextMessage(Utils.getString(Message.builder()
//                                    .type("room_full")
//                                    .sender(userUUID).build())));
//                            return;
//                        }
//
//                        // 여분의 자리가 있다면 해당 방 배열에 추가
//                        Map<String, String> userDetail = new HashMap<>();
//                        userDetail.put("id", userUUID);
//                        roomInfo.get(roomId).add(userDetail);
//                        log.info(">>> [ws] #{}번 방의 유저들 {}", roomId, roomInfo.get(roomId));
//
//                    } else {
//
//                        // 방이 존재하지 않는다면 값을 생성하고 추가
//                        Map<String, String> userDetail = new HashMap<>();
//                        userDetail.put("id", userUUID);
//                        List<Map<String, String>> newRoom = new ArrayList<>();
//                        newRoom.add(userDetail);
////                        roomInfo.put(roomId, newRoom);
//                    }

                    // 세션 저장, user 정보 저장 -> 방 입장
                    sessions.put(userUUID, session);
//                    userInfo.put(userUUID, roomId);

                    // TODO
                    // gameroomservice로 보내야 하는것
                    // gameroomId, token, sessionId
                    // gameroomId와 token에 있는 userId로 GameRoomRepository에서 해당 gameRoomUser 데이터를 찾고
                    // websessionId column에 접속한 sessionId를 update
                    gameRoomService.webSessionIdUpdate(roomId, token, userUUID);


                    // 해당 방에 다른 유저가 있었다면 offer-answer 를 위해 유저 리스트를 만들어 클라이언트에 전달

                    // roomInfo = { 방번호 : [ { id : userUUID1 }, { id: userUUID2 }, …], 방번호 : [ { id : userUUID3 }, { id: userUUID4 }, …], ... }
                    // originRoomUser -> 본인을 제외한 해당 방의 다른 유저들
                    List<Map<String, String>> originRoomUser = gameRoomService.getAllGameRoomUsersExceptMe(roomId, userUUID);
//                    for (Map<String, String> userDetail : roomInfo.get(roomId)) {
//
//                        // userUUID 가 본인과 같지 않다면 list 에 추가
//                        if (!(userDetail.get("id").equals(userUUID))) {
//                            Map<String, String> userMap = new HashMap<>();
//                            userMap.put("id", userDetail.get("id"));
//                            originRoomUser.add(userMap);
//                        }
//                    }

                    log.info(">>> [ws] 본인 {} 을 제외한 #{}번 방의 다른 유저들 {}", userUUID, roomId, originRoomUser);

                    // all_users 라는 타입으로 메시지 전달
                    session.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_ALL_USERS)
                            .allUsers(originRoomUser)
                            .sender(userUUID).build())));
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
//        String roomId = userInfo.get(userUUID); // roomId

        // TODO
        // userUUID를 가지고 GameRoomService 가서
        // 너 맞니? 그래 나가~ 하면 됨

        gameRoomService.exitGameRoom(null, null, userUUID);

        // 연결이 종료되면 sessions 와 userInfo 에서 해당 유저 삭제
        sessions.remove(userUUID);
//        userInfo.remove(userUUID);

        // roomInfo = { 방번호 : [ { id : userUUID1 }, { id: userUUID2 }, …], 방번호 : [ { id : userUUID3 }, { id: userUUID4 }, …], ... }
        // 해당하는 방의 value 인 user list 의 element 의 value 가 현재 userUUID 와 같다면 roomInfo 에서 remove
//        List<Map<String, String>> removed = new ArrayList<>();
//        roomInfo.get(roomId).forEach(s -> {
//            try {
//                if(s.containsValue(userUUID)) {
//                    removed.add(s);
//                }
//            }
//            catch (Exception e) {
//                log.info(">>> 에러 발생 : if문 생성 실패 {}", e.getMessage());
//            }
//        });
//        roomInfo.get(roomId).removeAll(removed);

        // 본인을 제외한 모든 유저에게 user_exit 라는 타입으로 메시지 전달
        sessions.values().forEach(s -> {
            try {
                if(!(s.getId().equals(userUUID))) {
                    s.sendMessage(new TextMessage(Utils.getString(Message.builder()
                            .type(MSG_TYPE_EXIT)
                            .sender(userUUID).build())));
                }
            }
            catch (Exception e) {
                log.info(">>> 에러 발생 : user_exit 메시지 전달 실패 {}", e.getMessage());
            }

        });

//        log.info(">>> [ws] #{}번 방에서 {} 삭제 완료", roomId, userUUID);
//        log.info(">>> [ws] #{}번 방에 남은 유저 {}", roomId, roomInfo.get(roomId));
    }

    // 소켓 통신 에러
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.info(">>> 에러 발생 : 소켓 통신 에러 {}", exception.getMessage());
    }
}
