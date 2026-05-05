package com.sdnah.Ticket_Management_System_.Application_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

public interface IrepresnteUserService {
    Member requireMember(String token);
    String requireMemberId(String token);
    boolean validateToken(String token);
}
