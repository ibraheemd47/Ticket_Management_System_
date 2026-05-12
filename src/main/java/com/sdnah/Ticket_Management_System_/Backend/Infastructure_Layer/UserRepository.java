package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

public interface UserRepository extends JpaRepository<Member, String> {
	Optional<Member> findByUsername(String username);

	boolean existsByUsername(String username);
	Optional<Member> findByEmail(String email);
	
	Member findByMemberId(String memberId);
} 