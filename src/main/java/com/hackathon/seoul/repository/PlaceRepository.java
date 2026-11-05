package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByCourseIdOrderByOrderNoAsc(Long courseId);
}
