package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.DemoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", mock ? "mock" : "real");
    }

    /** 리허설: 새 유저 + 첫 가챠(서대문구)까지 한 번에 */
    @PostMapping("/demo/start")
    public DemoStartResponse start(@RequestBody(required = false) CreateUserRequest req) {
        return demoService.start(req);
    }

    /** 리허설: 모든 유저 기록 초기화 */
    @PostMapping("/demo/reset")
    public Map<String, String> reset() {
        demoService.resetAll();
        return Map.of("message", "초기화 완료");
    }
}
