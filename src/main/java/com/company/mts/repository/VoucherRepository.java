package com.company.mts.repository;

import com.company.mts.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByActiveTrueOrderByPointCostAsc();
    Optional<Voucher> findByCode(String code);
    List<Voucher> findByCategoryAndActiveTrue(String category);
}
