package com.sdnah.Ticket_Management_System_.Infastructure_Layer;


import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCompanyRepository implements ICompanyRepository {

    // Storage for companies, using a thread-safe map
    private final Map<Integer, Company> companies = new ConcurrentHashMap<>();

    @Override
    public void save(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Cannot save a null company.");
        }
        // Atomic put operation
        companies.put(company.getCompanyId(), company);
    }

    @Override
    public Optional<Company> findById(int companyId) {
        // Returns an Optional to handle null safety gracefully
        return Optional.ofNullable(companies.get(companyId));
    }

    @Override
    public Collection<Company> findAll() {
        // Returns an unmodifiable view to protect the internal state
        return Collections.unmodifiableCollection(companies.values());
    }

    @Override
    public void deleteById(int companyId) {
        companies.remove(companyId);
    }

    @Override
    public boolean existsById(int companyId) {
        return companies.containsKey(companyId);
    }   
}
