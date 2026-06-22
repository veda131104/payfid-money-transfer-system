package com.company.mts.service;

import com.company.mts.dto.VoucherClaimRequest;
import com.company.mts.dto.VoucherDTO;
import com.company.mts.dto.VoucherRedemptionDTO;
import com.company.mts.entity.RewardAccount;
import com.company.mts.entity.Voucher;
import com.company.mts.entity.VoucherRedemption;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.RewardAccountRepository;
import com.company.mts.repository.VoucherRedemptionRepository;
import com.company.mts.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final RewardAccountRepository rewardAccountRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          VoucherRedemptionRepository redemptionRepository,
                          RewardAccountRepository rewardAccountRepository) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardAccountRepository = rewardAccountRepository;
    }

    public List<VoucherDTO> getAvailableVouchers(Long accountId) {
        int totalPoints = rewardAccountRepository.findByAccountId(accountId)
                .map(RewardAccount::getTotalPoints)
                .orElse(0);

        return voucherRepository.findByActiveTrueOrderByPointCostAsc()
                .stream()
                .map(v -> {
                    VoucherDTO dto = toVoucherDTO(v);
                    dto.setEffectiveCost(applyTierDiscount(v.getPointCost(), totalPoints));
                    dto.setCanAfford(totalPoints >= dto.getEffectiveCost());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public VoucherRedemptionDTO redeemVoucher(VoucherClaimRequest request) {
        Long voucherId = request.getVoucherId();
        Long accountId = request.getAccountId();

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + voucherId));

        if (!voucher.isActive()) {
            throw new IllegalStateException("Voucher is no longer available");
        }

        if (voucher.getStock() <= 0) {
            throw new IllegalStateException("Voucher is out of stock");
        }

        RewardAccount rewardAccount = rewardAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("No reward account for account: " + accountId));

        int effectiveCost = applyTierDiscount(voucher.getPointCost(), rewardAccount.getTotalPoints());

        if (rewardAccount.getTotalPoints() < effectiveCost) {
            throw new IllegalStateException("Insufficient points. Need " + effectiveCost + " but have " + rewardAccount.getTotalPoints());
        }

        rewardAccount.deductPoints(effectiveCost);
        rewardAccountRepository.save(rewardAccount);

        voucher.setStock(voucher.getStock() - 1);
        voucherRepository.save(voucher);

        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setAccountId(accountId);
        redemption.setVoucherId(voucher.getId());
        redemption.setVoucherCode(voucher.getCode());
        redemption.setVoucherName(voucher.getName());
        redemption.setPointsSpent(effectiveCost);
        redemption.setRedemptionCode(UUID.randomUUID().toString());
        redemption.setRedeemedAt(LocalDateTime.now());
        redemption.setStatus("ACTIVE");

        VoucherRedemption saved = redemptionRepository.save(redemption);

        log.info("Account {} redeemed voucher '{}' for {} points. Redemption code: {}",
                accountId, voucher.getName(), effectiveCost, saved.getRedemptionCode());

        return toRedemptionDTO(saved);
    }

    public List<VoucherRedemptionDTO> getRedemptionHistory(Long accountId) {
        return redemptionRepository.findByAccountIdOrderByRedeemedAtDesc(accountId)
                .stream()
                .map(this::toRedemptionDTO)
                .collect(Collectors.toList());
    }

    private int applyTierDiscount(int baseCost, int totalPoints) {
        if (totalPoints >= 500) {
            return (int) Math.round(baseCost * 0.80);
        } else if (totalPoints >= 200) {
            return (int) Math.round(baseCost * 0.85);
        } else if (totalPoints >= 100) {
            return (int) Math.round(baseCost * 0.90);
        } else if (totalPoints >= 50) {
            return (int) Math.round(baseCost * 0.95);
        }
        return baseCost;
    }

    private VoucherDTO toVoucherDTO(Voucher v) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(v.getId());
        dto.setCode(v.getCode());
        dto.setName(v.getName());
        dto.setDescription(v.getDescription());
        dto.setPointCost(v.getPointCost());
        dto.setCashValue(v.getCashValue());
        dto.setCategory(v.getCategory());
        dto.setIcon(v.getIcon());
        dto.setActive(v.isActive());
        dto.setStock(v.getStock());
        return dto;
    }

    private VoucherRedemptionDTO toRedemptionDTO(VoucherRedemption r) {
        VoucherRedemptionDTO dto = new VoucherRedemptionDTO();
        dto.setId(r.getId());
        dto.setAccountId(r.getAccountId());
        dto.setVoucherId(r.getVoucherId());
        dto.setVoucherCode(r.getVoucherCode());
        dto.setVoucherName(r.getVoucherName());
        dto.setPointsSpent(r.getPointsSpent());
        dto.setRedemptionCode(r.getRedemptionCode());
        dto.setRedeemedAt(r.getRedeemedAt());
        dto.setStatus(r.getStatus());
        return dto;
    }
}
