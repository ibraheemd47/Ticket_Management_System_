package com.sdnah.Ticket_Management_System_.Application_Layer;

import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

import ch.qos.logback.core.subst.Token;

@Service
public class SystemAdminService {
    UserRepository userRepository;
    SystemAdminRepository systemAdminRepository;
    TokenRepository tokenRepository;

    public SystemAdminService(UserRepository userRepository, SystemAdminRepository systemAdminRepository,
            TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.tokenRepository = tokenRepository;

    }

    public void assign_system_admin(String token, String target_member_id) {
        AuthToken user_token = tokenRepository.findByToken(token);
        if (user_token == null) {
            throw new IllegalArgumentException("Invalid token for admin ");
        }
        if(!is_admin(user_token.getMemberId())){
            throw new IllegalArgumentException("Only system admins can assign new admins");
        }
        Member to_assign = userRepository.findById(target_member_id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        if (to_assign == null) {//NOTE : if we didnt want to save the admin as member delete that 
            throw new IllegalArgumentException("member not found");
        }
        if(is_admin(target_member_id)){
            throw new IllegalArgumentException("Member is already an admin");
        }


            // Create a new System_admin entity and save it to the database
        System_admin new_admin = new System_admin(to_assign);
        systemAdminRepository.save(new_admin);
        userRepository.save((Member) new_admin);//dub
        
    }

    private boolean is_admin(String memberId) {
        return systemAdminRepository.existsById(memberId);
    }

    public void close_company(String token, String company_id) {
        // Implementation for closing a company
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void open_company(String token, String company_id) { // FIXME:maybe we dont need
        // Implementation for opening a company
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
