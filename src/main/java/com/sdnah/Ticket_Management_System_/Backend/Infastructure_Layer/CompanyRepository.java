package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>{

    Optional<Company> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);

    List<Company> findByCompanyNameContainingIgnoreCase(String keyword);
}