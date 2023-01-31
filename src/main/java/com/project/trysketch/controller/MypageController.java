package com.project.trysketch.controller;

import com.project.trysketch.dto.request.UserRequestDto;
import com.project.trysketch.dto.response.AchievementResponseDto;
import com.project.trysketch.entity.ImageLike;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.service.ImageService;
import com.project.trysketch.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MypageController {

    private final ImageService imageService;
    private final MypageService mypageService;

    // 마이페이지에서 좋아요 누른 사진 조회
    @GetMapping("/image-like") // 수정 추가 김재영 01.31
    public ResponseEntity<Page<ImageLike>> getImage(@PageableDefault(size = 12,sort = "createdAt",direction = Sort.Direction.DESC) Pageable pageable,
                                                    HttpServletRequest request) {
        return ResponseEntity.ok(imageService.getImage(request, pageable));
    }
    // 마이페이지 회원조회
    @GetMapping
    public ResponseEntity<DataMsgResponseDto> getMyPage(HttpServletRequest request) {
        return ResponseEntity.ok(mypageService.getMyPage(request));
    }

    // 마이페이지 정보변경
    @PatchMapping("/profile")
    public ResponseEntity<DataMsgResponseDto> patchMyPage(@RequestBody @Valid UserRequestDto userRequestDto, HttpServletRequest request) {
        return ResponseEntity.ok(mypageService.patchMyPage(userRequestDto, request));
    }

    // 유저 뱃지 조회
    @GetMapping("/badge")
    public ResponseEntity<DataMsgResponseDto> getBadge(HttpServletRequest request) {
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.OK, mypageService.getBadge(request)));
    }


}
