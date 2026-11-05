package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    List<UserCourse> findByUserIdOrderByDrawnAtAsc(Long userId);
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
}
