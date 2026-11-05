package com.hackathon.seoul.service;

import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 2-1 지역 퀘스트 지도, 3-1 퀘스트 시작 안내 + 해금 상태 계산 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final Finder finder;
    private final PlaceRepository placeRepo;
    private final QuestRepository questRepo;
    private final QuestProgressRepository progressRepo;

    public CourseService(Finder finder, PlaceRepository placeRepo, QuestRepository questRepo,
                         QuestProgressRepository progressRepo) {
        this.finder = finder;
        this.placeRepo = placeRepo;
        this.questRepo = questRepo;
        this.progressRepo = progressRepo;
    }

    /** 2-1: 장소 4개 + 상태 + 캐릭터 위치(currentPlaceId) + 지역 완료 여부 */
    public CourseMapResponse courseMap(Long userId, Long courseId) {
        AppUser user = finder.user(userId);
        Course course = finder.course(courseId);
        UserCourse uc = finder.drawnCourse(userId, courseId);

        List<PlaceCard> cards = placeCards(userId, courseId);
        Long current = cards.stream()
                .filter(c -> "AVAILABLE".equals(c.status()))
                .map(PlaceCard::id).findFirst()
                .orElseGet(() -> cards.stream().filter(c -> "DONE".equals(c.status()))
                        .reduce((a, b) -> b).map(PlaceCard::id)
                        .orElse(cards.isEmpty() ? null : cards.get(0).id()));

        return new CourseMapResponse(course.getId(), course.getRegion().getCode(), course.getRegion().getName(),
                course.getCategory().name(), course.getCategory().getLabel(), course.getOpenCopy(),
                user.getCoins(), current, uc.isCompleted(), cards);
    }

    /** 3-1: 장소 시작 화면. 퀘스트 목록(순차 해금), 진행률 n/4 */
    public PlaceDetail placeDetail(Long userId, Long placeId) {
        AppUser user = finder.user(userId);
        Place place = finder.place(placeId);
        finder.drawnCourse(userId, place.getCourse().getId());

        List<Quest> quests = questRepo.findByPlaceIdOrderByOrderNoAsc(placeId);
        Map<Long, QuestProgress> progress = progressByQuest(userId, placeId);

        List<QuestCard> cards = new ArrayList<>();
        for (Quest q : quests) {
            cards.add(new QuestCard(q.getId(), q.getCode(), q.getOrderNo(), q.getName(), q.getTitle(), q.getStory(),
                    questStatus(q, quests, progress)));
        }
        int done = (int) cards.stream().filter(c -> "DONE".equals(c.status())).count();
        Long current = cards.stream().filter(c -> "AVAILABLE".equals(c.status())).map(QuestCard::id).findFirst().orElse(null);

        return new PlaceDetail(place.getId(), place.getCourse().getId(), place.getCourse().getRegion().getName(),
                place.getCourse().getCategory().getLabel(), place.getName(), place.getTitle(), place.getStory(),
                user.getCoins(), done, quests.size(), done + "/" + quests.size(), current, cards);
    }

    // ===== 공용 계산 =====

    List<PlaceCard> placeCards(Long userId, Long courseId) {
        List<Place> places = placeRepo.findByCourseIdOrderByOrderNoAsc(courseId);
        Map<Long, List<Quest>> questsByPlace = questRepo.findByPlaceCourseId(courseId).stream()
                .collect(Collectors.groupingBy(q -> q.getPlace().getId()));
        Set<Long> doneQuestIds = progressRepo.findByUserIdAndQuestPlaceCourseId(userId, courseId).stream()
                .filter(QuestProgress::isDone).map(p -> p.getQuest().getId()).collect(Collectors.toSet());

        List<PlaceCard> cards = new ArrayList<>();
        boolean allPreviousDone = true;
        for (Place p : places) {
            List<Quest> qs = questsByPlace.getOrDefault(p.getId(), List.of());
            int total = qs.size();
            int done = (int) qs.stream().filter(q -> doneQuestIds.contains(q.getId())).count();
            String status;
            if (total == 0) status = "COMING_SOON";
            else if (done >= total) status = "DONE";
            else if (allPreviousDone) status = "AVAILABLE";
            else status = "LOCKED";
            cards.add(new PlaceCard(p.getId(), p.getOrderNo(), p.getName(), p.getRepresentative(), p.getCandidates(),
                    p.getPinKey(), p.getImageKey(), status, done, total));
            if (!"DONE".equals(status) && !"COMING_SOON".equals(status)) allPreviousDone = false;
        }
        return cards;
    }

    Map<Long, QuestProgress> progressByQuest(Long userId, Long placeId) {
        return progressRepo.findByUserIdAndQuestPlaceId(userId, placeId).stream()
                .collect(Collectors.toMap(p -> p.getQuest().getId(), Function.identity()));
    }

    /** LOCKED / AVAILABLE / DONE — 첫 퀘스트는 열림, 나머지는 이전 퀘스트 완료 시 */
    static String questStatus(Quest q, List<Quest> placeQuests, Map<Long, QuestProgress> progress) {
        QuestProgress p = progress.get(q.getId());
        if (p != null && p.isDone()) return "DONE";
        Optional<Quest> prev = placeQuests.stream().filter(x -> x.getOrderNo() < q.getOrderNo())
                .max(Comparator.comparingInt(Quest::getOrderNo));
        if (prev.isEmpty()) return "AVAILABLE";
        QuestProgress pp = progress.get(prev.get().getId());
        return (pp != null && pp.isDone()) ? "AVAILABLE" : "LOCKED";
    }

    static Effect parseEffect(String raw) {
        int i = raw.indexOf(':');
        return i < 0 ? new Effect(raw.trim(), null) : new Effect(raw.substring(0, i).trim(), raw.substring(i + 1).trim());
    }
}
