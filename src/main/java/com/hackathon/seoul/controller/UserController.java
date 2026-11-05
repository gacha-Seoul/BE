package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.GachaService;
import com.hackathon.seoul.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final GachaService gachaService;

    public UserController(UserService userService, GachaService gachaService) {
        this.userService = userService;
        this.gachaService = gachaService;
    }

    @PostMapping
    public UserResponse create(@RequestBody(required = false) CreateUserRequest req) {
        return userService.create(req);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable Long userId) {
        return userService.get(userId);
    }

    /** 1-1 메인 가챠 화면: 코인, 뽑기 n회 남음, 후보 카드 5개 */
    @GetMapping("/{userId}/main")
    public MainResponse main(@PathVariable Long userId) {
        return gachaService.main(userId);
    }

    /** 뽑기 (무료 → 코인 순, 재뽑기 불가) */
    @PostMapping("/{userId}/gacha")
    public GachaResponse gacha(@PathVariable Long userId) {
        return gachaService.draw(userId);
    }
}
