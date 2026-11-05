package com.hackathon.seoul.repository;
import com.hackathon.seoul.domain.GachaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface GachaHistoryRepository extends JpaRepository<GachaHistory, Long> {
    Optional<GachaHistory> findByUserIdAndRequestId(Long userId, String requestId);
}
