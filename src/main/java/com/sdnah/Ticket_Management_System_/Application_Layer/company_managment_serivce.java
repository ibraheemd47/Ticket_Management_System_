package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;

public class company_managment_serivce {

    private final ICompanyRepository companyRepository;
    private final PolicyService policyService; // הקשר ל-PolicyService לפי התרשים

    @Autowired
    public company_managment_serivce(ICompanyRepository companyRepository, PolicyService policyService) {
        this.companyRepository = companyRepository;
        this.policyService = policyService;
    }

    // --- II.2.1: View Active Production Companies ---
    public List<Company> getActiveCompanies() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .toList();
    }

    // --- II.3.2: Open Production Company (Triggered by II.1.1) ---
    public void openCompany(int companyId, String name, int founderId) {
        if (companyRepository.existsById(companyId)) {
            throw new IllegalStateException("Company ID already exists.");
        }
        Company newCompany = new Company(companyId, name, founderId);
        companyRepository.save(newCompany);
    }

    

    // --- II.4.1: Manage Events (Add/Remove) ---
    public void addEvent(int actingUserId, int companyId, int eventId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        company.addEvent(eventId);
        companyRepository.save(company);
    }

    public void removeEvent(int actingUserId, int companyId, int eventId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        company.removeEvent(eventId);
        companyRepository.save(company);
    }

    // --- II.4.3: Change Purchase and Discount Policies ---
    public void updateCompanyPolicies(int actingUserId, int companyId, int newPolicyId) {
        Company company = getCompanyOrThrow(companyId);
        
        if (company.getCompanyFounderId() != actingUserId) {
            throw new SecurityException("Only the founder is authorized to update company policies.");
        }

        policyService.updateCompanyPolicy(companyId, newPolicyId);
    }

    // --- II.4.5: View Company Purchase and Order History ---
    public List<Integer> getPurchaseHistory(int actingUserId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        return company.getPurchaseHistoryIds();
    }

    public List<Integer> getOrderHistory(int actingUserId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        return company.getOrderHistoryIds();
    }

    // --- II.4.7: View and Appoint Company Managers ---
    public void appointManager(int actingUserId, int companyId, int newManagerId) {
        Company company = getCompanyOrThrow(companyId);
        if (company.getCompanyFounderId() != actingUserId) {
            throw new SecurityException("Only the founder can appoint managers.");
        }
        company.appointManager(newManagerId);
        companyRepository.save(company);
    }

    // // --- II.4.13 & II.4.14: Set Company Status (Open/Close) ---
    // public void setCompanyStatus(int actingUserId, int companyId, boolean open) {
    //     Company company = getCompanyOrThrow(companyId);
    //     if (company.getCompanyFounderId() != actingUserId) {
    //         throw new SecurityException("Only the founder can open or close the company.");
    //     }
    //     company.setOpen(open);
    //     companyRepository.save(company);
    // }

    // --- II.4.4: Respond to Inquiries ---
    public void respondToInquiry(int actingUserId, int companyId, int inquiryId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        // לוגיקה לטיפול בפנייה...
    }

    // --- Internal Helpers ---
    private Company getCompanyOrThrow(int companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company ID " + companyId + " not found."));
    }

    private void validateManagerOrFounder(int userId, Company company) {
        boolean isFounder = company.getCompanyFounderId() == userId;
        boolean isManager = company.getManagers().contains(userId);
        if (!isFounder && !isManager) {
            throw new SecurityException("Unauthorized: User " + userId + " is not a manager/owner of this company.");
        }
        }
        
    }
