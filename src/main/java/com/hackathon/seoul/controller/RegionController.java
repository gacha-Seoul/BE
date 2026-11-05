package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.RegionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    /** 지역 상세 + 장소·퀘스트 + 내 진행 상황 */
    @GetMapping("/{regionId}")
    public RegionDetail detail(@PathVariable Long regionId, @RequestParam Long userId) {
        return regionService.detail(userId, regionId);
    }
}
