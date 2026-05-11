package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;

public interface SystemAdminRepository extends JpaRepository<System_admin, String> {


    public boolean existsByMemberId(String memberId);
    

}
