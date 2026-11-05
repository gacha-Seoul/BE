package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByRegionIdOrderByOrderNoAsc(Long regionId);
    long countByRegionId(Long regionId);
}
