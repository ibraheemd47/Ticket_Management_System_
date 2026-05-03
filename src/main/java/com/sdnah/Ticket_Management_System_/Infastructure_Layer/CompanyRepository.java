package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByCompanyId(int companyId);

    boolean existsByCompanyId(int companyId);
}