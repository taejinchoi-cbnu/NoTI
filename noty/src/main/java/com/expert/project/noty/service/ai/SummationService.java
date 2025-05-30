package com.expert.project.noty.service.ai;

import com.expert.project.noty.config.PromptProperties;
import com.expert.project.noty.dto.ai.currentserver.GeminiPromptRequest;
import com.expert.project.noty.dto.ai.currentserver.SummationResponse;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.entity.SummationEntity;
import com.expert.project.noty.exception.ResourceNotFoundException;
import com.expert.project.noty.repository.AudioFileRepository;
import com.expert.project.noty.repository.SummationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SummationService {

    private final GeminiService geminiService;
    private final SummationRepository summationRepository;
    private final AudioFileRepository audioFileRepository;
//    private final PromptProperties promptProperties;

    public SummationService(GeminiService geminiService,
                            SummationRepository summationRepository,
                            AudioFileRepository audioFileRepository,
                            PromptProperties promptProperties) {
        this.geminiService = geminiService;
        this.summationRepository = summationRepository;
        this.audioFileRepository = audioFileRepository;
//        this.promptProperties = promptProperties;
    }

    public SummationResponse processGeminiRequest(String userId, String savedFileName) {

        // savedFileName으로 audio 파일 찾기
        AudioFileEntity audioFileEntity = audioFileRepository.findBySavedName(savedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("Audio file not found"));

        // 해당 사용자와 오디오 ID에 맞는 Summation 조회
        SummationEntity summationEntity = summationRepository.findByUserIdAndAudioId(
                        userId, audioFileEntity.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Summation not found"));

        if (summationEntity.getStt() == null) {
            System.out.println("stt is null");
            return new SummationResponse("null");
        }

        String example = "오늘 회의는 신규 프로젝트 킥오프를 위한 방향성과 초기 업무 분담을 논의하기 위한 자리로 시작됐다. 우선 프로젝트의 목적과 배경에 대한 간단한 설명이 이루어졌다. 이번 프로젝트는 기존 고객사의 요청에 의해 개편되는 모바일 앱 개발 프로젝트이며, 사용성 개선과 성능 최적화가 핵심 목표다.\n" +
                "\n" +
                "먼저 일정에 대한 개괄적인 계획이 제시되었다. 전체 프로젝트는 약 3개월로 예상되며, 1개월은 기획과 설계, 1개월은 개발, 마지막 1개월은 테스트 및 배포 단계로 구성될 예정이다. 다만 고객사 쪽에서 기획안을 빠르게 보고 싶어 하기 때문에, 내부적으로 2주 내에 초기 와이어프레임을 완성하는 것이 중요하다는 언급이 있었다.\n" +
                "\n" +
                "다음으로, 주요 기능 정의에 대한 이야기가 나왔다. 사용자가 가장 많이 불편함을 호소했던 로그인 절차 간소화, 홈 화면 개인화, 알림 설정 기능 개선이 우선순위로 꼽혔다. 특히 기존 로그인 방식이 너무 복잡하다는 피드백이 많았기 때문에, 간편 로그인 도입 여부가 핵심 토의 주제였다. 이에 대해 \"소셜 로그인만으로 충분한가?\"라는 질문이 제기되었고, 금융 정보 접근을 위해 OTP 또는 생체 인증 도입도 검토해야 한다는 의견이 나왔다.\n" +
                "\n" +
                "개발 프레임워크와 기술 스택에 대해서도 논의가 이어졌다. 백엔드는 기존과 동일하게 Spring Boot를 유지하되, 프론트엔드는 React Native 대신 Flutter로 전환하는 것을 제안하는 목소리가 나왔다. 이에 대해 성능과 유지보수 측면에서 어떤 선택이 더 유리한지 비교 분석을 진행한 후 결정하기로 합의되었다. 또한, 코드 품질 관리와 협업을 위해 Git Flow 전략을 유지하며, CI/CD 파이프라인을 간소화하는 방안도 함께 검토되기로 했다.\n" +
                "\n" +
                "업무 분장에 있어서는 기획 파트에서는 김 팀장이 전체 흐름을 잡고, 박 주임이 UI/UX 리서치를 진행하기로 했다. 개발 측에서는 이 대리가 프론트엔드 프로토타입을, 최 사원이 백엔드 API 정의 작업을 맡기로 했다. QA 관련 부분은 아직 인력 배정이 되지 않았으며, 다음 주 회의에서 구체적인 역할을 정하기로 했다.\n" +
                "\n" +
                "고객 커뮤니케이션 방식에 대해서도 정리가 필요하다는 지적이 있었다. 고객사 담당자가 자주 변경되어 일관된 피드백이 어려웠던 과거 사례를 언급하며, 이번에는 정해진 주기로 문서화된 리포트를 제공하고, 주요 의사결정은 서면으로 남기는 절차를 도입하기로 의견이 모아졌다.\n" +
                "\n" +
                "회의 중간에는 디자인 관련 이슈가 잠시 언급되었다. 이전 앱 디자인은 트렌드에 뒤처졌다는 평가가 많았기 때문에, 이번에는 Material Design이나 iOS Human Interface Guideline에 충실한 설계를 목표로 하자는 의견이 나왔다. 다만 지나치게 최신 트렌드만 좇기보다 사용자 연령대와 사용 목적에 맞게 안정감을 주는 UI를 구성하자는 의견도 덧붙여졌다.\n" +
                "\n" +
                "마지막으로 전체 일정과 다음 회의 일정이 확인되었다. 이번 주 금요일까지 초기 요구사항 정리를 완료하고, 다음 주 월요일에는 와이어프레임 1차 초안을 공유하는 것을 목표로 한다. 회의 시간은 월요일 오전 10시로 잠정 확정되었고, 필요시 슬랙으로 사전 논의를 진행하기로 했다.\n" +
                "\n";

        // 전체 프롬프트 생성
        String defaultPrompot = "[역할] 당신은 회의록 작성 전문가입니다. [맥락] 다음은 팀 회의 녹취록입니다. [지시] 이 회의의 핵심 내용을 요약해 주세요. [목적] 팀원이 결과를 빠르게 공유하고 다음 단계를 준비할 수 있도록 합니다. [형식] * 주요 논의 주제 * 결정된 사항(있다면) * 주요 액션 아이템(담당자 포함) * 기타 주요 의견 * 시간별 순서 * 약속 시간(있다면) [길이] 회의 길이에 비례 [포함/제외] 상세 설명 대신 핵심 아이디어와 결론 중심, 잡담/주제 이탈 내용 제외 [스타일] 명확하고 간결한 비즈니스 스타일로 작성, 텍스트 파일로 봤을 때 잘 정돈된 형태\n내용:";
        String fullPrompt = defaultPrompot + summationEntity.getStt();

        System.out.println("prompt: " + fullPrompt);

        String summationResult = geminiService.callGemini(fullPrompt);
        System.out.println("summation result: " + summationResult);

        summationEntity.setSummation(summationResult);
        summationRepository.save(summationEntity);

        return new SummationResponse(summationResult);
    }
}
