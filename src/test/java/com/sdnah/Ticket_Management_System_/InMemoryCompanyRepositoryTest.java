package com.sdnah.Ticket_Management_System_;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCompanyRepositoryTest {

    private ICompanyRepository repository;
    private Company company;

    @BeforeEach
    void setUp() {
        repository = new TestCompanyRepository();
        company = new Company(1, "Test Company", 100);
    }

    @Test
    void GivenCompany_WhenSave_ThenCanFindById() {
        repository.save(company);

        Optional<Company> result = repository.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getCompanyId());
        assertEquals("Test Company", result.get().getCompanyName());
    }

    @Test
    void GivenSavedCompany_WhenExistsById_ThenReturnTrue() {
        repository.save(company);

        assertTrue(repository.existsById(1));
    }

    @Test
    void GivenMissingCompany_WhenFindById_ThenReturnEmpty() {
        assertTrue(repository.findById(999).isEmpty());
        assertFalse(repository.existsById(999));
    }

    @Test
    void GivenSeveralCompanies_WhenFindAll_ThenReturnAll() {
        repository.save(company);
        repository.save(new Company(2, "Second Company", 200));

        Collection<Company> allCompanies = repository.findAll();

        assertEquals(2, allCompanies.size());
    }

    @Test
    void GivenSavedCompany_WhenDeleteById_ThenCompanyRemoved() {
        repository.save(company);

        repository.deleteById(1);

        assertFalse(repository.existsById(1));
        assertTrue(repository.findById(1).isEmpty());
    }

    @Test
    void GivenSameCompanyId_WhenSaveAgain_ThenCompanyUpdated() {
        repository.save(company);
        Company updated = new Company(1, "Updated Company", 100);

        repository.save(updated);

        assertEquals(1, repository.findAll().size());
        assertEquals("Updated Company",
                repository.findById(1).get().getCompanyName());
    }

    private static class TestCompanyRepository implements ICompanyRepository {
        private final Map<Integer, Company> companies = new ConcurrentHashMap<>();

        @Override
        public void save(Company company) {
            companies.put(company.getCompanyId(), company);
        }

        @Override
        public Optional<Company> findById(int companyId) {
            return Optional.ofNullable(companies.get(companyId));
        }

        @Override
        public Collection<Company> findAll() {
            return new ArrayList<>(companies.values());
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
}