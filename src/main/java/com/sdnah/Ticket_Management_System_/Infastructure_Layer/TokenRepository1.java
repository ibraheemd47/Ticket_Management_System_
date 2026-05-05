package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;

@Repository
public interface TokenRepository1 extends JpaRepository<AuthToken, String> {

    AuthToken findByTokenValue(String token);

    boolean existsByTokenValue(String token);

    @Transactional
    void deleteByTokenValue(String token);

    List<AuthToken> findAllByMemberId(String memberId);
}
