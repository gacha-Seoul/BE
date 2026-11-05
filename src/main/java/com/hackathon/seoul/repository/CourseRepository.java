package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByOrderByOrderNoAsc();
}
