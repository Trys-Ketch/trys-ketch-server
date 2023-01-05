package com.project.trysketch.image;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageLikeService {

    private final AmazonS3Service s3Service;
    //숙소 정보 작성
    public MsgResponseDto saveLike(List<MultipartFile> multipartFilelist, User user) throws IOException {

        if (multipartFilelist != null) {
            s3Service.upload(multipartFilelist, "static", user);

        }
        return new MsgResponseDto(StatusMsgCode.DONE_LIKE);
    }


}