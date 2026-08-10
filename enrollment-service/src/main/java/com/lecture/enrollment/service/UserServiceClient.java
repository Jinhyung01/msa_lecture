package com.lecture.enrollment.service;

import com.lecture.enrollment.exception.EnrollmentApiException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.user-service.url:http://user-service:8081}")
    private String userServiceUrl;

    /** Gateway가 전달한 사용자 ID를 신뢰 가능한 실제 역할로 변환한다. */
    public String getRole(Long userId) {
        return getProfile(userId).getRole();
    }

    /** 신청 목록에 표시할 이름·사번(현재 email 필드)과 역할을 함께 조회한다. */
    public UserProfile getProfile(Long userId) {
        try {
            UserProfile profile = webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/api/users/internal/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserProfile.class)
                    .block();

            if (profile == null || profile.getId() == null || profile.getRole() == null) {
                throw new IllegalStateException("User Service 응답에 사용자 역할이 없습니다.");
            }
            if (!userId.equals(profile.getId())) {
                throw new IllegalStateException("User Service 응답의 사용자 ID가 일치하지 않습니다.");
            }
            return profile;
        } catch (Exception e) {
            log.error("[UserServiceClient] 사용자 정보 조회 실패 - userId: {}, error: {}",
                    userId, e.getMessage());
            throw EnrollmentApiException.internalError("사용자 정보를 확인하지 못했습니다.");
        }
    }

    @Getter
    @NoArgsConstructor
    public static class UserProfile {
        private Long id;
        private String email;
        private String name;
        private String role;
    }
}
