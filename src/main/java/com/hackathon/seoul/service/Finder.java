package com.hackathon.seoul.service;

import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 자주 쓰는 조회 + 404/403 처리 */
@Component
public class Finder {
    private final AppUserRepository userRepo;
    private final CourseRepository courseRepo;
    private final PlaceRepository placeRepo;
    private final QuestRepository questRepo;
    private final UserCourseRepository userCourseRepo;

    public Finder(AppUserRepository userRepo, CourseRepository courseRepo, PlaceRepository placeRepo,
                  QuestRepository questRepo, UserCourseRepository userCourseRepo) {
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.placeRepo = placeRepo;
        this.questRepo = questRepo;
        this.userCourseRepo = userCourseRepo;
    }

    public AppUser user(Long id) {
        if (id == null) throw new GameException(HttpStatus.BAD_REQUEST, "userId가 필요해요.");
        return userRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "유저가 없어요: " + id));
    }

    public Course course(Long id) {
        return courseRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "코스가 없어요: " + id));
    }

    public Place place(Long id) {
        return placeRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "장소가 없어요: " + id));
    }

    public Quest quest(Long id) {
        return questRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "퀘스트가 없어요: " + id));
    }

    /** 가챠로 뽑지 않은 코스는 플레이 불가 */
    public UserCourse drawnCourse(Long userId, Long courseId) {
        return userCourseRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new GameException(HttpStatus.FORBIDDEN, "아직 가챠로 뽑지 않은 지역이에요."));
    }
}
