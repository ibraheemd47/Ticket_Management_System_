package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
@Repository
public interface TokenRepository extends JpaRepository<AuthToken, String> {
    
    AuthToken findBytokenValue(String token);

    boolean existsBytokenValue(String token);

    boolean deleteBytokenValue(String token);
}
