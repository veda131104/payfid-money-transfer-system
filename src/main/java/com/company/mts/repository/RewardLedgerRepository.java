package com.company.mts.repository;

import com.company.mts.entity.RewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    List<RewardLedger> findByAccountIdOrderByGrantedAtDesc(Long accountId);

    Optional<RewardLedger> findByTransactionId(Long transactionId);

    boolean existsByTransactionId(Long transactionId);

    @Query("SELECT COALESCE(SUM(r.pointsAwarded), 0) FROM RewardLedger r WHERE r.accountId = :accountId")
    Integer sumPointsByAccountId(@Param("accountId") Long accountId);
}
