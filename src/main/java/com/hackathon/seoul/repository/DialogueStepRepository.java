package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.DialogueStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DialogueStepRepository extends JpaRepository<DialogueStep, Long> {
    List<DialogueStep> findByQuestIdOrderByStepNoAsc(Long questId);
}
