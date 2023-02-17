package com.project.trysketch.global.utill;

import com.project.trysketch.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 1. 기능   : Scheduler
// 2. 작성자 : 황미경
@Slf4j
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final ImageService imageService;

    // 좋아요 안 눌린 그림 DB, S3로 부터 주기적으로 삭제하는 기능. 매일 0시 0분
    @Scheduled(cron = "0 0 4 * * *")               // 초, 분, 시, 일, 월, 주 순서
    public void deleteImage() {
        imageService.deleteImage();
        log.info("좋아요 안눌린 이미지 삭제.");
    }
}