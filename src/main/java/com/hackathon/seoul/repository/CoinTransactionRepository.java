package com.hackathon.seoul.repository;
import com.hackathon.seoul.domain.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    boolean existsByEventKey(String eventKey);
    List<CoinTransaction> findByUserIdOrderByIdAsc(Long userId);
}
