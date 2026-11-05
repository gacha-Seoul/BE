package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.CourseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** 2-1 지역 퀘스트 지도: 장소 4개, 캐릭터 위치, 지역 완료 여부 */
    @GetMapping("/courses/{courseId}")
    public CourseMapResponse course(@PathVariable Long courseId, @RequestParam Long userId) {
        return courseService.courseMap(userId, courseId);
    }

    /** 3-1 퀘스트 시작 안내: 제목, 스토리, Q1~Q4 상태, 진행률 n/4 */
    @GetMapping("/places/{placeId}")
    public PlaceDetail place(@PathVariable Long placeId, @RequestParam Long userId) {
        return courseService.placeDetail(userId, placeId);
    }
}
