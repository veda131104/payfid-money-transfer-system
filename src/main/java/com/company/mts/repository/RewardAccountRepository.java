package com.company.mts.repository;

import com.company.mts.entity.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {

    Optional<RewardAccount> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);
}
