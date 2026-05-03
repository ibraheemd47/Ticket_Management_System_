package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

public class Checkout_Domain_Service {
    private Member member;
    private Policy policy;

    public Checkout_Domain_Service(Member member, Policy policy) {
        this.member = member;
        this.policy = policy;
    }
}
