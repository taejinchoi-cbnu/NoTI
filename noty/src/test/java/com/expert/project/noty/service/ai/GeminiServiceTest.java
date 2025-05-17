package com.expert.project.noty.service.ai;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.expert.project.noty.service.ai.GeminiService.*;

import java.io.IOException;

@SpringBootTest
@Transactional
public class GeminiServiceTest {

    @Autowired
    private GeminiService geminiService;

    @Test
    public void Test() {
        String result = geminiService.callGemini("안녕하세요");

        System.out.println("## 회의록 요약\n\n**회의 목적:** 신규 모바일 앱 개발 프로젝트 킥오프, 방향성 논의 및 초기 업무 분담\n\n**프로젝트 개요:**\n\n*   **목표:** 기존 고객사 모바일 앱 개편, 사용성 개선 및 성능 최적화\n*   **일정:** 약 3개월 (기획/설계 1개월, 개발 1개월, 테스트/배포 1개월), 2주 내 초기 와이어프레임 완성\n\n**주요 논의 내용:**\n\n*   **주요 기능:**\n    *   로그인 절차 간소화 (간편 로그인 도입 검토, OTP/생체 인증 필요성 논의)\n    *   홈 화면 개인화\n    *   알림 설정 기능 개선\n*   **기술 스택:**\n    *   백엔드: Spring Boot 유지\n    *   프론트엔드: React Native vs. Flutter 비교 분석 후 결정\n    *   Git Flow 유지, CI/CD 파이프라인 간소화 검토\n*   **업무 분장:**\n    *   기획: 김 팀장 (전체 흐름), 박 주임 (UI/UX 리서치)\n    *   개발: 이 대리 (프론트엔드 프로토타입), 최 사원 (백엔드 API 정의)\n    *   QA: 다음 주 회의에서 역할 분담\n*   **고객 커뮤니케이션:**\n    *   정기적인 문서화된 리포트 제공\n    *   주요 의사결정 서면 기록\n*   **디자인:**\n    *   Material Design 또는 iOS Human Interface Guideline 기반 설계\n    *   사용자 연령대 및 사용 목적 고려, 안정감 있는 UI 구성\n\n**결정 사항 및 향후 계획:**\n\n*   이번 주 금요일까지 초기 요구사항 정리 완료\n*   다음 주 월요일 오전 10시, 와이어프레임 1차 초안 공유\n*   필요시 슬랙으로 사전 논의\n\n**미결정 사항:**\n\n*   프론트엔드 프레임워크 (React Native vs. Flutter)\n*   QA 담당자\n\n**핵심 키워드:** 모바일 앱 개편, 사용성 개선, 성능 최적화, 와이어프레임, 간편 로그인, 프레임워크 선택, 업무 분장, 고객 커뮤니케이션, 디자인 가이드라인\n");
    }
}
