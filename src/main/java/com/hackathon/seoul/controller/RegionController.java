package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.seoul.service.RegionService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "02. 지역 상세", description = "뽑은 지역에 어떤 장소와 퀘스트가 있는지 확인합니다.")
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    /** 지역 상세 + 장소·퀘스트 + 내 진행 상황 */
    @Operation(summary = "지역의 장소 · 퀘스트 목록 조회",
            description = "지역 선택 화면에서 호출합니다. stages는 장소, 각 장소의 quests는 순서대로 진행할 과제입니다. 퀘스트 status는 LOCKED / AVAILABLE / IN_PROGRESS / COMPLETED입니다. 기본 시드에서 서대문구는 regionId=13, 전통시장은 stageId=5입니다. 미해금 지역도 목록 조회는 가능하지만 플레이는 해금 후 가능합니다. 없는 사용자·지역은 404입니다.")
    @GetMapping("/{regionId}")
    public RegionDetail detail(@Parameter(description = "서울 구 ID. 기본 서대문구는 13", example = "13") @PathVariable Long regionId, @Parameter(description = "사용자 생성 또는 demo/start 응답의 사용자 ID", example = "1") @RequestParam Long userId) {
        return regionService.detail(userId, regionId);
    }
}
