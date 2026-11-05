package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.seoul.service.GachaService;
import com.hackathon.seoul.service.RegionService;
import com.hackathon.seoul.service.UserService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "01. 사용자 · 지도 · 가챠", description = "사용자를 생성하고 코인 잔액, 서울 지도 상태, 지역 뽑기를 조회·실행합니다.")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RegionService regionService;
    private final GachaService gachaService;

    public UserController(UserService userService, RegionService regionService, GachaService gachaService) {
        this.userService = userService;
        this.regionService = regionService;
        this.gachaService = gachaService;
    }

    @Operation(summary = "사용자 생성 · 최초 코인 1개 지급",
            description = "게임에 처음 들어올 때 한 번 호출합니다. 반환된 id를 userId로 저장해 이후 요청에 사용하세요. 본문을 생략하면 Traveler / English / BEGINNER로 생성합니다.")
    @PostMapping
    public UserResponse create(@RequestBody(required = false) CreateUserRequest req) {
        return userService.create(req);
    }

    @Operation(summary = "사용자 정보 · 코인 잔액 조회",
            description = "사용자의 닉네임, 설명 언어, 한국어 수준과 현재 코인 잔액을 조회합니다. 없는 사용자면 404입니다.")
    @GetMapping("/{userId}")
    public UserResponse get(@Parameter(description = "사용자 생성 또는 demo/start 응답의 사용자 ID", example = "1") @PathVariable Long userId) {
        return userService.get(userId);
    }

    /** 3D 지도: 25개 구 상태 + 코인 */
    @Operation(summary = "서울 25개 구 지도 상태 조회",
            description = "3D 지도를 처음 표시하거나 가챠·퀘스트 완료 후 갱신할 때 호출합니다. regions의 status로 미해금(LOCKED), 도전 중(UNLOCKED), 정복(CONQUERED)을 구분하세요. playable=true인 지역만 뽑기 대상입니다.")
    @GetMapping("/{userId}/map")
    public MapResponse map(@Parameter(description = "사용자 생성 또는 demo/start 응답의 사용자 ID", example = "1") @PathVariable Long userId) {
        return regionService.map(userId);
    }

    /** 가챠 1회 */
    @Operation(summary = "가챠 · 코인 1개로 새 지역 뽑기",
            description = "코인 1개를 사용해 미보유 지역을 해금합니다. 기본 설정에서 첫 지역은 서대문구입니다. requestId는 뽑기를 누를 때 UUID로 만들고, 통신 재시도에는 같은 값을 보내세요. 같은 요청은 재차감하지 않고 최초 결과와 당시 잔액을 반환합니다. 새 뽑기는 새 requestId를 사용합니다. 코인 부족·후보 소진·잘못된 requestId는 400, 없는 사용자는 404입니다.")
    @PostMapping("/{userId}/gacha")
    public GachaResponse gacha(@Parameter(description = "사용자 생성 또는 demo/start 응답의 사용자 ID", example = "1") @PathVariable Long userId, @RequestBody GachaRequest req) {
        return gachaService.draw(userId, req.requestId());
    }
}
