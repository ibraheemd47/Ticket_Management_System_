package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>{

    Optional<Company> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);

    List<Company> findByCompanyNameContainingIgnoreCase(String keyword);

    /** Returns only open (active) companies — avoids loading the full table. */
    @Query("SELECT c FROM Company c WHERE c.isOpen = :open")
    List<Company> findByIsOpenTrue(@Param("open") boolean open);

    /** Open companies whose name contains the given keyword (case-insensitive). */
    @Query("SELECT c FROM Company c WHERE c.isOpen = :open AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Company> findActiveByNameContaining(@Param("name") String name, @Param("open") boolean open);
}