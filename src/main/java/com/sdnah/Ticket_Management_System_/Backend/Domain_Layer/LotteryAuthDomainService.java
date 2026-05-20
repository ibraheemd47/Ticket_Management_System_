package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

public class LotteryAuthDomainService {

    public void assertCanCreateLottery(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can create a lottery");
        }
    }

    public void assertCanDrawLottery(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can draw a lottery");
        }
    }

    public void assertCanRegisterToLottery(Member actor, Lottery lottery) {
        assertActiveMember(actor);

        if (!actor.isVerified()) {
            throw new RuntimeException("Only verified members can register to a lottery");
        }

        if (!lottery.isOpen()) {
            throw new RuntimeException("Lottery is not open for registration");
        }

        if (lottery.isAlreadyRegistered(actor.getMemberId())) {
            throw new RuntimeException("Member is already registered to this lottery");
        }
    }

    private void assertActiveMember(Member member) {
        if (member == null || !member.isActive()) {
            throw new RuntimeException("Member must be active to perform this action");
        }
    }

    private void requireCompany(Company company) {
        if (company == null) {
            throw new RuntimeException("Company is required");
        }
    }
}