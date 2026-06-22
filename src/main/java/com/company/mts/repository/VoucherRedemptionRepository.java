package com.company.mts.repository;

import com.company.mts.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {
    List<VoucherRedemption> findByAccountIdOrderByRedeemedAtDesc(Long accountId);
    long countByAccountId(Long accountId);
}
