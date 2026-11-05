package com.hackathon.seoul.service;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 리허설/발표용: 기록 초기화, 유저 생성 + 첫 뽑기를 한 번에 */
@Service
public class DemoService {

    private final ChatMessageRepository messageRepo;
    private final QuestProgressRepository progressRepo;
    private final UserCourseRepository userCourseRepo;
    private final AppUserRepository userRepo;
    private final UserService userService;
    private final GachaService gachaService;

    public DemoService(ChatMessageRepository messageRepo, QuestProgressRepository progressRepo,
                       UserCourseRepository userCourseRepo, AppUserRepository userRepo,
                       UserService userService, GachaService gachaService) {
        this.messageRepo = messageRepo;
        this.progressRepo = progressRepo;
        this.userCourseRepo = userCourseRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.gachaService = gachaService;
    }

    /** 모든 유저 기록 삭제 (코스·장소·퀘스트 데이터는 유지) */
    @Transactional
    public void resetAll() {
        messageRepo.deleteAllInBatch();
        progressRepo.deleteAllInBatch();
        userCourseRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    /** 새 유저 + 첫 뽑기(서대문구 · 문화) */
    @Transactional
    public DemoStartResponse start(CreateUserRequest req) {
        UserResponse user = userService.create(req);
        GachaResponse gacha = gachaService.draw(user.id());
        return new DemoStartResponse(userService.get(user.id()), gacha);
    }
}
