package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.util.UUID;

public interface PolicyService {

    double applyGeneralDiscounts(UUID eventId, double total, int itemCount);

    double calculateCouponDiscount(UUID eventId, double currentPrice, int itemCount, String couponCode);
}