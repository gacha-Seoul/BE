package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);

    /** 스테이지 데이터가 있는 지역만 (가챠 후보) */
    @Query("select distinct q.stage.region from Quest q")
    List<Region> findPlayableRegions();
}
