package com.project.trysketch.controller;

import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;


// 1. 기능   : ImageLike 컨트롤러
// 2. 작성자 : 황미경
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageController {
    private final ImageService imageService;

//    // 게임의 한 턴 끝날시 S3에 업로드
//    @PostMapping(value = "/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    public ResponseEntity<MsgResponseDto> saveImage(@RequestPart(value = "file") MultipartFile multipartFile,
//                                                    HttpServletRequest request) throws IOException {
//        return ResponseEntity.ok(imageService.saveImage(multipartFile, request));
//    }


//========하나로 합쳐주세요=========================================================================================
    
    // 그림 좋아요 기능
//    @PostMapping(value = "/image/like/{imageId}")
//    public ResponseEntity<DataMsgResponseDto> imageLike(@PathVariable Long imageId,
//                                                        HttpServletRequest request) {
//        return ResponseEntity.ok(imageService.likeImage(imageId, request));
//    }
    // 마이페이지에서 좋아요 누른 사진 취소
//    @PostMapping("/mypage/cancel-like/{imageId}")
//    public ResponseEntity<MsgResponseDto> cancelLike(@PathVariable Long imageId,
//                                                     HttpServletRequest request) {
//        return ResponseEntity.ok(imageService.cancelLike(imageId, request));
//    }
//--------합친 결과--------------------------------------------------------------------------------------
    @PostMapping("/image/like/{imageId}")
    public ResponseEntity<DataMsgResponseDto> imageLike(@PathVariable Long imageId,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(imageService.likeImage(imageId, request));
    }


}