package com.hackathon.seoul.service;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 리허설/발표용: 기록 초기화, 유저 생성 + 첫 가챠를 한 번에 */
@Service
public class DemoService {

    private final CoinTransactionRepository coins;
    private final GachaHistoryRepository history;
    private final ChatMessageRepository messageRepo;
    private final QuestProgressRepository progressRepo;
    private final UserRegionRepository userRegionRepo;
    private final AppUserRepository userRepo;
    private final UserService userService;
    private final GachaService gachaService;

    public DemoService(ChatMessageRepository messageRepo, QuestProgressRepository progressRepo,
                       UserRegionRepository userRegionRepo, AppUserRepository userRepo,
                       UserService userService, GachaService gachaService, CoinTransactionRepository coins, GachaHistoryRepository history) {
        this.coins = coins;
        this.history = history;
        this.messageRepo = messageRepo;
        this.progressRepo = progressRepo;
        this.userRegionRepo = userRegionRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.gachaService = gachaService;
    }

    /** 모든 유저 기록 삭제 (지역/스테이지 데이터는 유지) */
    @Transactional
    public void resetAll() {
        messageRepo.deleteAllInBatch();
        progressRepo.deleteAllInBatch();
        userRegionRepo.deleteAllInBatch();
        history.deleteAllInBatch();
        coins.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    /** 새 유저 생성 + 첫 가챠(서대문구) */
    @Transactional
    public DemoStartResponse start(CreateUserRequest req) {
        UserResponse user = userService.create(req);
        GachaResponse gacha = gachaService.draw(user.id());
        return new DemoStartResponse(userService.get(user.id()), gacha);
    }
}
