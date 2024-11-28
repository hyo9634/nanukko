package nanukko.nanukko_back.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nanukko.nanukko_back.config.NcpConfig;
import nanukko.nanukko_back.dto.file.FileDTO;
import nanukko.nanukko_back.dto.file.FileDirectoryType;
import nanukko.nanukko_back.util.ImageResizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ImageService {
    private final AmazonS3Client amazonS3Client; // NCP Object Storage 클라이언트
    private final NcpConfig ncpConfig;

    // 이미지 크기를 저장하기 위한 레코드
    private record ImageSize(int width, int height) {
    }

    // 이미지 타입별 크기 설정
    private static final Map<FileDirectoryType, ImageSize> IMAGE_SIZES = Map.of(
            FileDirectoryType.PROFILE, new ImageSize(200, 200),
            FileDirectoryType.CHAT, new ImageSize(300, 300),
            FileDirectoryType.SELL, new ImageSize(500, 500)
    );

    @Value("${image.compression.quality:0.7}")  // application.properties에서 설정, 기본값 0.7
    private float imageQuality;


    //단일 파일 업로드 처리 메서드
    // (이미지 수정 시 단일 파일 업로드가 필요할 수 있음)
    // MultipartFile을 받아서 NCP에 업로드하고 파일 정보를 담은 DTO 반환
    public FileDTO uploadSingleFile(MultipartFile file, FileDirectoryType directoryType, String userId) {
        try {
            // 원본 파일명 추출
            String originalFileName = file.getOriginalFilename();
            // UUID를 이용해 중복 없는 파일명 생성
            String uploadFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            // 버킷 내 저장 경로 설정 (예: 기본경로/아이디/년월일 폴더 구조)
            String uploadFilePath = directoryType.getDirectory() + "/" +
                    userId + "/" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 이미지 리사이징 및 압축
            ImageSize targetSize = IMAGE_SIZES.get(directoryType);
            byte[] resizedImageBytes = ImageResizer.resizeAndCompressImage(
                    file.getBytes(),
                    targetSize.width(),
                    targetSize.height(),
                    imageQuality
            );

            // ByteArrayInputStream 생성
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImageBytes);

            // 파일 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg"); // 압축 후에는 항상 JPEG
            metadata.setContentLength(resizedImageBytes.length); // 압축된 데이터의 실제 길이로 설정

            // 버킷에 파일 업로드 (경로/파일명 형식으로 저장)
            String keyName = uploadFilePath + "/" + uploadFileName;

            //PutObjectRequest를 사용하여 ACL 설정
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ncpConfig.getBucket(),
                    keyName,
                    inputStream,
                    metadata
            ).withCannedAcl(CannedAccessControlList.PublicRead); //공개 읽기 권한 설정

            //파일 업로드
            amazonS3Client.putObject(putObjectRequest);

            // inputStream 닫기
            inputStream.close();

            // 업로드된 파일의 접근 URL 생성
            String uploadFileUrl = amazonS3Client.getUrl(ncpConfig.getBucket(), keyName).toString();

            // 파일 정보를 DTO에 담아서 반환
            return FileDTO.builder()
                    .originalFileName(originalFileName)
                    .uploadFileName(uploadFileName)
                    .uploadFilePath(uploadFilePath)
                    .uploadFileUrl(uploadFileUrl)
                    .build();

        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    // 다중 파일 업로드 처리 메소드
    public List<FileDTO> uploadMultipleFiles(List<MultipartFile> files, FileDirectoryType directoryType, String userId) {
        // 빈 파일 필터링하고 각각의 파일을 uploadSingleFile 메소드를 통해 업로드
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> uploadSingleFile(file, directoryType, userId))
                .collect(Collectors.toList());
    }

    // 파일 삭제 (경로와 파일명으로 삭제)
    public void deleteFile(String filePath, String fileName) {
        try {
            // 전체 경로 생성 (경로/파일명)
            String keyName = filePath + "/" + fileName;
            // NCP Object Storage에서 파일 삭제
            amazonS3Client.deleteObject(ncpConfig.getBucket(), keyName);
            log.info("파일 삭제 완료: {}", keyName);
        } catch (AmazonServiceException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    // 여러 파일 삭제 메소드
    public void deleteFiles(List<FileDTO> files) {
        files.forEach(file ->
                deleteFile(file.getUploadFilePath(), file.getUploadFileName())
        );
    }

    // 파일의 URL 조회
    public String getFileUrl(String filePath, String fileName) {
        try {
            // 전체 경로로 파일 URL 반환
            return amazonS3Client.getUrl(ncpConfig.getBucket(), filePath + "/" + fileName).toString();
        } catch (Exception e) {
            log.error("파일 URL 조회 실패 - 경로: {}, 파일명: {}, error: {}", filePath, fileName, e.getMessage());
            throw new RuntimeException("파일 URL 조회 실패", e);
        }
    }
}