package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;

public interface SystemAdminRepository extends JpaRepository<System_admin, String> {


    

}
