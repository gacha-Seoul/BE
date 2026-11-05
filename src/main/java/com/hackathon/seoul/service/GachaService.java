package com.hackathon.seoul.service;

import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 1-1 메인 가챠 화면.
 * - 후보 카드 5개 (지역·카테고리) 중 아직 안 뽑은 것에서 랜덤 1개. 첫 뽑기는 first-course 고정
 * - 무료 횟수 먼저, 이후 코인 1개. 결과 저장과 차감을 한 트랜잭션에서
 * - 재뽑기 불가: 뽑은 뒤 퀘스트를 1개 이상 완료해야 다음 뽑기 가능 (draw-requires-quest)
 */
@Service
public class GachaService {

    private final Finder finder;
    private final CourseRepository courseRepo;
    private final QuestRepository questRepo;
    private final UserCourseRepository userCourseRepo;
    private final QuestProgressRepository progressRepo;
    private final CourseService courseService;
    private final GameProperties props;
    private final Random random = new SecureRandom();

    public GachaService(Finder finder, CourseRepository courseRepo, QuestRepository questRepo,
                        UserCourseRepository userCourseRepo, QuestProgressRepository progressRepo,
                        CourseService courseService, GameProperties props) {
        this.finder = finder;
        this.courseRepo = courseRepo;
        this.questRepo = questRepo;
        this.userCourseRepo = userCourseRepo;
        this.progressRepo = progressRepo;
        this.courseService = courseService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public MainResponse main(Long userId) {
        AppUser user = finder.user(userId);
        List<UserCourse> drawn = userCourseRepo.findByUserIdOrderByDrawnAtAsc(userId);
        Map<Long, UserCourse> drawnById = drawn.stream().collect(Collectors.toMap(uc -> uc.getCourse().getId(), uc -> uc));

        List<CandidateCard> cards = courseRepo.findAllByOrderByOrderNoAsc().stream()
                .map(c -> card(c, drawnById.get(c.getId()))).toList();
        String blocked = blockedReason(user, drawn);
        Long current = drawn.isEmpty() ? null : drawn.get(drawn.size() - 1).getCourse().getId();

        return new MainResponse(userId, user.getCoins(), user.getFreeDraws(), user.getDrawsLeft(),
                blocked == null, blocked, current, cards);
    }

    @Transactional
    public GachaResponse draw(Long userId) {
        AppUser user = finder.user(userId);
        List<UserCourse> drawn = userCourseRepo.findByUserIdOrderByDrawnAtAsc(userId);

        String blocked = blockedReason(user, drawn);
        if (blocked != null) {
            String msg = switch (blocked) {
                case "NO_DRAWS" -> "뽑기 횟수가 없어요. 퀘스트를 완료해서 코인을 모아 보세요.";
                case "COMPLETE_A_QUEST" -> "재뽑기는 안 돼요. 퀘스트를 완료하면 다시 뽑을 수 있어요.";
                default -> "모든 지역을 뽑았어요!";
            };
            throw new GameException(HttpStatus.BAD_REQUEST, msg);
        }

        Set<Long> owned = drawn.stream().map(uc -> uc.getCourse().getId()).collect(Collectors.toSet());
        List<Course> candidates = courseRepo.findAllByOrderByOrderNoAsc().stream()
                .filter(c -> !owned.contains(c.getId())).toList();

        Course picked = null;
        if (drawn.isEmpty()) picked = firstCourse(candidates);
        if (picked == null) picked = candidates.get(random.nextInt(candidates.size()));

        user.payForDraw(props.drawCost);
        UserCourse uc = userCourseRepo.save(new UserCourse(user, picked));

        return new GachaResponse(card(picked, uc), picked.getOpenCopy(),
                courseService.placeCards(userId, picked.getId()),
                user.getCoins(), user.getFreeDraws(), user.getDrawsLeft());
    }

    /** null 이면 뽑기 가능 */
    private String blockedReason(AppUser user, List<UserCourse> drawn) {
        long total = courseRepo.count();
        if (drawn.size() >= total) return "ALL_DRAWN";
        if (!user.canPay(props.drawCost)) return "NO_DRAWS";
        if (props.drawRequiresQuest && !drawn.isEmpty()) {
            LocalDateTime last = drawn.get(drawn.size() - 1).getDrawnAt();
            boolean questAfter = progressRepo.findByUserId(user.getId()).stream()
                    .anyMatch(p -> p.isDone() && p.getCompletedAt() != null && p.getCompletedAt().isAfter(last));
            if (!questAfter) return "COMPLETE_A_QUEST";
        }
        return null;
    }

    private Course firstCourse(List<Course> candidates) {
        if (props.firstCourse == null || !props.firstCourse.contains("-")) return null;
        String[] parts = props.firstCourse.split("-", 2);
        return candidates.stream()
                .filter(c -> c.getRegion().getCode().equalsIgnoreCase(parts[0])
                        && c.getCategory().name().equalsIgnoreCase(parts[1]))
                .findFirst().orElse(null);
    }

    private CandidateCard card(Course c, UserCourse uc) {
        String status = uc == null ? "NOT_DRAWN" : (uc.isCompleted() ? "COMPLETED" : "DRAWN");
        boolean playable = !questRepo.findByPlaceCourseId(c.getId()).isEmpty();
        Region r = c.getRegion();
        return new CandidateCard(c.getId(), r.getId(), r.getCode(), r.getName(), r.getNameEn(),
                c.getCategory().name(), c.getCategory().getLabel(), c.getImageKey(), status, playable);
    }
}
