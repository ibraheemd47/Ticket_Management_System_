package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;

@Repository
public interface CompanyRepository 
        extends JpaRepository<Company, Integer>, ICompanyRepository {
        
                Company findByCompanyId(int companyId);

}