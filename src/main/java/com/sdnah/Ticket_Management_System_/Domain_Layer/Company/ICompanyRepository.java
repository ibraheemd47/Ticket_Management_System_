package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;
import java.util.Collection;
import java.util.Optional;

/**
 * Interface for Company persistence management.
 * Defined in the Domain Layer to enable Dependency Inversion.
 */
public interface ICompanyRepository {
    Company save(Company company);
    Optional<Company> findById(int companyId);
    Collection<Company> findAll();
    void deleteById(int companyId);
    boolean existsById(int companyId);
}