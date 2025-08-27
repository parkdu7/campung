package com.example.campung.record.service;

import com.example.campung.content.service.S3Service;
import com.example.campung.entity.Record;
import com.example.campung.entity.User;
import com.example.campung.record.dto.RecordCreateRequest;
import com.example.campung.record.dto.RecordCreateResponse;
import com.example.campung.record.dto.RecordDeleteResponse;
import com.example.campung.record.repository.RecordRepository;
import com.example.campung.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @Transactional
    public RecordCreateResponse createRecord(RecordCreateRequest request, String accessToken) throws IOException {
        System.out.println("=== 녹음파일 등록 시작 ===");
        System.out.println("accessToken: " + accessToken);

        // 입력 검증
        if (request.getAudioFile() == null || request.getAudioFile().isEmpty()) {
            return new RecordCreateResponse(false, "녹음 파일이 필요합니다");
        }

        // 음성 파일 용량 제한 검사 (10MB)
        if (request.getAudioFile().getSize() > 10 * 1024 * 1024) {
            return new RecordCreateResponse(false, "음성 파일은 10MB를 초과할 수 없습니다.");
        }

        // 오디오 파일 형식 검증
        String contentType = request.getAudioFile().getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            return new RecordCreateResponse(false, "오디오 파일만 업로드 가능합니다");
        }

        try {
            // 사용자 조회/생성
            User user = userRepository.findByUserId(accessToken)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .userId(accessToken)
                                .nickname(accessToken)
                                .passwordHash("temp_hash")
                                .build();
                        return userRepository.save(newUser);
                    });

            // S3에 오디오 파일 업로드
            String audioUrl = s3Service.uploadFile(request.getAudioFile());
            System.out.println("오디오 파일 업로드 완료: " + audioUrl);

            // Record 엔티티 생성
            Record.RecordBuilder recordBuilder = Record.builder()
                    .user(user)
                    .recordUrl(audioUrl);

            // 위치 정보가 있으면 추가
            if (request.getLatitude() != null && request.getLongitude() != null) {
                recordBuilder.latitude(BigDecimal.valueOf(request.getLatitude()))
                            .longitude(BigDecimal.valueOf(request.getLongitude()));
            }

            Record record = recordBuilder.build();
            Record savedRecord = recordRepository.save(record);

            System.out.println("=== 녹음파일 DB 저장 완료 ===");
            System.out.println("저장된 Record ID: " + savedRecord.getRecordId());

            return new RecordCreateResponse(true, "녹음파일이 성공적으로 등록되었습니다", savedRecord.getRecordId());

        } catch (IOException e) {
            System.err.println("파일 업로드 실패: " + e.getMessage());
            return new RecordCreateResponse(false, "파일 업로드에 실패했습니다");
        } catch (Exception e) {
            System.err.println("녹음파일 등록 실패: " + e.getMessage());
            return new RecordCreateResponse(false, "녹음파일 등록에 실패했습니다");
        }
    }

    @Transactional
    public RecordDeleteResponse deleteRecord(Long recordId, String accessToken) {
        System.out.println("=== 녹음파일 삭제 시작 ===");
        System.out.println("recordId: " + recordId + ", accessToken: " + accessToken);

        try {
            // 사용자 조회
            Optional<User> userOpt = userRepository.findByUserId(accessToken);
            if (userOpt.isEmpty()) {
                return new RecordDeleteResponse(false, "사용자를 찾을 수 없습니다");
            }

            User user = userOpt.get();

            // 녹음 파일 조회 (작성자 확인)
            Optional<Record> recordOpt = recordRepository.findByRecordIdAndUser(recordId, user);
            if (recordOpt.isEmpty()) {
                return new RecordDeleteResponse(false, "삭제 권한이 없거나 녹음파일을 찾을 수 없습니다");
            }

            Record record = recordOpt.get();

            // DB에서 삭제
            recordRepository.delete(record);

            System.out.println("=== 녹음파일 삭제 완료 ===");
            System.out.println("삭제된 Record ID: " + recordId);

            return new RecordDeleteResponse(true, "녹음파일이 성공적으로 삭제되었습니다");

        } catch (Exception e) {
            System.err.println("녹음파일 삭제 실패: " + e.getMessage());
            return new RecordDeleteResponse(false, "녹음파일 삭제에 실패했습니다");
        }
    }
}