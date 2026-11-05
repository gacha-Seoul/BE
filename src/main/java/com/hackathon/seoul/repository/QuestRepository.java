package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findByPlaceIdOrderByOrderNoAsc(Long placeId);
    List<Quest> findByPlaceCourseId(Long courseId);
}
