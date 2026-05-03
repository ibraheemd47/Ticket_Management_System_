package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PolicyServiceImpl implements PolicyService {

    @Override
    public double applyGeneralDiscounts(UUID eventId, double total, int itemCount) {
        return total;
    }

    @Override
    public double calculateCouponDiscount(UUID eventId, double currentPrice, int itemCount, String couponCode) {
        return currentPrice;
    }
}
