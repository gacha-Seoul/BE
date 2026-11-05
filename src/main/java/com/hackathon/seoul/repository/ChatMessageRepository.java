package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdAndStageIdOrderByIdAsc(Long userId, Long stageId);

    @Modifying
    @Query("delete from ChatMessage m where m.user.id = :userId and m.stage.id = :stageId")
    void deleteHistory(@Param("userId") Long userId, @Param("stageId") Long stageId);
}
