package com.project.trysketch.controller;

import com.project.trysketch.dto.response.DataMsgResponseDto;
import com.project.trysketch.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

// 1. 기능   : ImageLike 컨트롤러
// 2. 작성자 : 황미경, 김재영
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageController {
    private final ImageService imageService;
    @PostMapping("/image/like/{imageId}")
    public ResponseEntity<DataMsgResponseDto> imageLike(@PathVariable Long imageId, HttpServletRequest request) {
        return ResponseEntity.ok(imageService.likeImage(imageId, request));
    }
}