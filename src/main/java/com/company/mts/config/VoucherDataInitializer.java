package com.company.mts.config;

import com.company.mts.entity.Voucher;
import com.company.mts.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("!test")
public class VoucherDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VoucherDataInitializer.class);

    private final VoucherRepository voucherRepository;

    public VoucherDataInitializer(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Override
    public void run(String... args) {
        if (voucherRepository.count() > 0) {
            return;
        }

        log.info("Seeding voucher catalog...");

        List<Voucher> vouchers = List.of(
            new Voucher("AMZ-500", "Amazon Pay Voucher - ₹500", "Shop millions of products on Amazon India", 50, new BigDecimal("500"), "SHOPPING", "shopping_cart"),
            new Voucher("AMZ-1000", "Amazon Pay Voucher - ₹1000", "Get ₹1000 Amazon gift card for premium shopping", 95, new BigDecimal("1000"), "SHOPPING", "shopping_cart"),
            new Voucher("FLP-750", "Flipkart Gift Card - ₹750", "Use on Flipkart for electronics, fashion & more", 70, new BigDecimal("750"), "SHOPPING", "local_mall"),
            new Voucher("SWG-200", "Swiggy Food Voucher - ₹200", "Enjoy delicious meals delivered to your doorstep", 20, new BigDecimal("200"), "FOOD", "restaurant"),
            new Voucher("SWG-500", "Swiggy Food Voucher - ₹500", "Feast with family - ₹500 Swiggy credit", 48, new BigDecimal("500"), "FOOD", "restaurant"),
            new Voucher("ZOM-300", "Zomato Dining Credit - ₹300", "Dine out or order in with Zomato", 28, new BigDecimal("300"), "FOOD", "ramen_dining"),
            new Voucher("UBR-250", "Uber Ride Credit - ₹250", "Travel anywhere in the city with Uber", 24, new BigDecimal("250"), "TRAVEL", "directions_car"),
            new Voucher("UBR-500", "Uber Travel Credit - ₹500", "Extra long rides or multiple trips", 47, new BigDecimal("500"), "TRAVEL", "directions_car"),
            new Voucher("OLA-200", "Ola Cab Credit - ₹200", "Book Ola rides across India", 19, new BigDecimal("200"), "TRAVEL", "local_taxi"),
            new Voucher("NET-300", "Netflix Subscription - 1 Month", "Watch movies, shows & documentaries ad-free", 30, new BigDecimal("300"), "ENTERTAINMENT", "smart_display"),
            new Voucher("PRM-500", "Amazon Prime Video - 3 Months", "Unlimited streaming of movies & exclusive series", 48, new BigDecimal("500"), "ENTERTAINMENT", "play_circle"),
            new Voucher("HOT-250", "Hotstar Premium - 1 Month", "Live sports, movies & Hotstar specials", 25, new BigDecimal("250"), "ENTERTAINMENT", "live_tv"),
            new Voucher("BMS-400", "BookMyShow Voucher - ₹400", "Book movie tickets & live events", 38, new BigDecimal("400"), "ENTERTAINMENT", "movie"),
            new Voucher("IRCTC-300", "IRCTC Rail Wallet - ₹300", "Book train tickets with zero convenience fee", 28, new BigDecimal("300"), "TRAVEL", "train"),
            new Voucher("DOM-200", "Dominos Pizza Voucher - ₹200", "Delicious pizzas, sides & desserts", 18, new BigDecimal("200"), "FOOD", "local_pizza")
        );

        voucherRepository.saveAll(vouchers);
        log.info("Seeded {} vouchers into the catalog", vouchers.size());
    }
}
