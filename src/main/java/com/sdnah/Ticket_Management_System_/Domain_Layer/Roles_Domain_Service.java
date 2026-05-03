package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

public class Roles_Domain_Service {

    private Company company;
    private Member user;

    public Roles_Domain_Service(Company company,Member user) {
        this.company = company;
        this.user = user;
    }
 
}
