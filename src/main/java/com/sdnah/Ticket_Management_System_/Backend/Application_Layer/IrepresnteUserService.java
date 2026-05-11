package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

public interface IrepresnteUserService {
    Member requireMember(String token);
    String requireMemberId(String token);
    boolean validateToken(String token);
}
