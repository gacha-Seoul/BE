package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {
    List<UserRegion> findByUserId(Long userId);
    Optional<UserRegion> findByUserIdAndRegionId(Long userId, Long regionId);
}
