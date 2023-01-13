package com.project.trysketch.image;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.project.trysketch.entity.GameFlow;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.GameFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;


// 1. 기능   : S3 업로드 로직
// 2. 작성자 : 황미경
@Slf4j
@Component
@RequiredArgsConstructor
@Service
public class AmazonS3Service {

    private final AmazonS3Client amazonS3Client;
    private final ImageRepository imageRepository;
    private final GameFlowRepository gameFlowRepository;

    @Value("${cloud.aws.s3.bucket}")         // bucket 이름
    public String bucket;

    // 이미지 업로드 (S3, DB)
    public void upload(MultipartFile multipartFile, String dirName, User user, int round, int keywordIndex, Long roomId) throws IOException {
        if (multipartFile != null) {
            File uploadFile = convert(multipartFile).orElseThrow(() -> new IllegalArgumentException("파일 전환 실패"));

            Image image = new Image(upload(uploadFile, dirName), user.getNickname());

            GameFlow gameFlow = GameFlow.builder()
                    .roomId(roomId)
                    .round(round)
                    .keywordIndex(keywordIndex)
                    .imagePath(image.getPath())
                    .nickname(user.getNickname())
                    .build();

            imageRepository.save(image);
            gameFlowRepository.save(gameFlow);
        }
    }

    // S3로 파일 업로드 (파일이름 지정, 로컬파일 삭제 + 파일 업로드 메서드 호출)
    private String upload(File uploadFile, String dirName) {
        String fileName = dirName + "/" + UUID.randomUUID();     // S3에 저장될 파일 이름
        String uploadImageUrl = putS3(uploadFile, fileName);     // s3로 업로드
        removeNewFile(uploadFile);                               // 로컬에 저장된 파일 지우기
        return uploadImageUrl;
    }

    // S3로 실제 파일 업로드
    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    // S3 이미지 삭제
    public void delete(String fileName){
        DeleteObjectRequest request = new DeleteObjectRequest(bucket, fileName);
        amazonS3Client.deleteObject(request);
    }

    // 로컬에 저장된 이미지 지우기
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }

    private Optional<File> convert(MultipartFile multipartFile) throws IOException {
        File convertFile = new File(multipartFile.getOriginalFilename());
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) { // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장하기 위함
                fos.write(multipartFile.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }
}