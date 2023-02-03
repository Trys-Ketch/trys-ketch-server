package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.entity.GameFlowCount;
import com.project.trysketch.repository.GameFlowCountRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MessageSendingThread extends Thread{
    private final int roundMaxNum;
    private final GameFlowRequestDto requestDto;
    private final GameFlowCountRespository gameFlowCountRespository;
    private final SimpMessageSendingOperations sendingOperations;

    @Override
    public void run() {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [MessageSendingThread - sendAllSubmitMessage] >>>>>>>>>>>>>>>>>>>>>>>>");
        GameFlowCount gameFlowCount = gameFlowCountRespository.findByRoomIdAndRound(
                requestDto.getRoomId(),
                requestDto.getRound()
        );
        log.info(">>>>>>> [GameService - sendAllSubmitMessage] ================gameFlowCount.getGameFlowCount() if문 밖 {}", gameFlowCount.getGameFlowCount());
        if (gameFlowCount.getGameFlowCount() != 0 && gameFlowCount.getGameFlowCount() == roundMaxNum) {
            // 이미지 라운드 인지 키워드 라운드 인지 판별 후 destination 부여
            String destination = requestDto.getImage() == null || requestDto.getImage().length() == 0 ? "word" : "image";
            // 전체가 제출 했다면 모두에게 메시지 전송
            Map<String, Object> allSubmitMessage = new HashMap<>();
            allSubmitMessage.put("completeSubmit", true);
            sendingOperations.convertAndSend("/topic/game/submit-" + destination + "/" + requestDto.getRoomId(), allSubmitMessage);
            log.info(">>>>>>> [GameService - isAllSubmit] 전체 제출 후 메시지 전송 성공! : {}", allSubmitMessage);
            log.info(">>>>>>>>>>>>>>>>>>>>>>>> [MessageSendingThread - sendAllSubmitMessage] 완료 >>>>>>>>>>>>>>>>>>>>>>>>");
        } else {
            run();
            log.info(">>>>>>>>>>>>>>>>>>>>>>>> [MessageSendingThread - sendAllSubmitMessage] 다시시작 >>>>>>>>>>>>>>>>>>>>>>>>");
        }
    }
}
