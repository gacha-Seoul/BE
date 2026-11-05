package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.GachaService;
import com.hackathon.seoul.service.RegionService;
import com.hackathon.seoul.service.UserService;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public UserResponse create(@RequestBody(required = false) CreateUserRequest req) {
        return userService.create(req);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable Long userId) {
        return userService.get(userId);
    }

    /** 3D 지도: 25개 구 상태 + 코인 */
    @GetMapping("/{userId}/map")
    public MapResponse map(@PathVariable Long userId) {
        return regionService.map(userId);
    }

    /** 가챠 1회 */
    @PostMapping("/{userId}/gacha")
    public GachaResponse gacha(@PathVariable Long userId) {
        return gachaService.draw(userId);
    }
}
