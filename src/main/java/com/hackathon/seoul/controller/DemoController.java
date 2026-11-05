package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.seoul.service.DemoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "05. 데모 · 서버 상태", description = "연동 확인과 발표 리허설용 API입니다. 초기화 API는 모든 사용자의 플레이 기록을 삭제합니다.")
@RestController
@RequestMapping("/api")
public class DemoController {

    private final DemoService demoService;
    private final boolean mock;

    public DemoController(DemoService demoService, @Value("${llm.mock:true}") boolean mock) {
        this.demoService = demoService;
        this.mock = mock;
    }

    /** 서버 상태 + AI 모드 확인 (프론트 연결 확인용) */
    @Operation(summary = "서버 연결 · AI 모드 확인",
            description = "프론트에서 서버에 연결되는지 확인할 때 호출합니다. status=UP이면 응답 가능하며, llmMode=mock은 고정 시나리오, real은 실제 AI 모드입니다. 외부 AI 호출 성공 여부를 검사하는 API는 아닙니다.")
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", mock ? "mock" : "real");
    }

    /** 리허설: 새 유저 + 첫 가챠(서대문구)까지 한 번에 */
    @Operation(summary = "데모 바로 시작 · 사용자 생성 + 첫 지역 뽑기",
            description = "발표용 빠른 시작입니다. 사용자 생성과 첫 뽑기를 한 번에 처리합니다. 기본 설정에서는 최초 코인 1개가 첫 뽑기에 사용되어 잔액 0개, 서대문구 해금 상태로 시작합니다. 응답 user.id를 저장한 뒤 장소 5 조회 → 퀘스트 501 start → chat 순서로 호출하세요. 호출할 때마다 새 사용자가 생깁니다. 본문은 생략 가능합니다.")
    @PostMapping("/demo/start")
    public DemoStartResponse start(@RequestBody(required = false) CreateUserRequest req) {
        return demoService.start(req);
    }

    /** 리허설: 모든 유저 기록 초기화 */
    @Operation(summary = "전체 사용자 플레이 기록 초기화 · 주의",
            description = "리허설 데이터를 전부 지울 때만 사용하세요. 모든 사용자의 계정·진행·대화·코인 내역·뽑기 기록을 삭제합니다. 지역·장소·퀘스트 콘텐츠는 유지됩니다. 삭제 후 기존 userId는 사용할 수 없으므로 demo/start로 새로 시작하세요. 정상 프론트 플레이 흐름에서는 호출하지 않습니다.")
    @PostMapping("/demo/reset")
    public Map<String, String> reset() {
        demoService.resetAll();
        return Map.of("message", "초기화 완료");
    }
}
