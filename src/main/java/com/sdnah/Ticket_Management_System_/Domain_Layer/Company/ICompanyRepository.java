package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;
import java.util.Collection;
import java.util.Optional;

/**
 * Interface for Company persistence management.
 * Defined in the Domain Layer to enable Dependency Inversion.
 */
public interface ICompanyRepository {

    /**
     * Saves a new company or updates an existing one.
     * Version 1 Constraint: Implementation must be atomic and thread-safe.
     * @param company The company entity to persist.
     */
    void save(Company company);
    /**
     * Finds a company by its unique numeric identifier.
     * @param companyId The ID of the company.
     * @return An Optional containing the company if found, or empty if not.
     */
    Optional<Company> findById(int companyId);

    /**
     * Retrieves all companies in the system.
     * Used for Use Case II.2.1: View Active Production Companies and Their Events.
     * @return A collection of all companies.
     */
    Collection<Company> findAll();

    /**
     * Deletes a company from the system by its ID.
     * @param companyId The ID of the company to delete.
     */
    void deleteById(int companyId);

    /**
     * Checks if a company with the given ID exists in the system.
     * @param companyId The ID to check.
     * @return true if the company exists, false otherwise.
     */
    boolean existsById(int companyId);
}