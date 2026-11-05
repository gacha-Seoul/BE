package com.hackathon.seoul.repository;
import com.hackathon.seoul.domain.QuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface QuestProgressRepository extends JpaRepository<QuestProgress, Long> {
    Optional<QuestProgress> findByUserIdAndQuestId(Long userId, Long questId);
    List<QuestProgress> findByUserId(Long userId);
    List<QuestProgress> findByUserIdAndQuestStageId(Long userId, Long stageId);
    long countByUserIdAndQuestStageRegionIdAndStatus(Long userId, Long regionId, QuestProgress.Status status);
}
