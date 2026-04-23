package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.*;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;

public class company_managment_serivce {

    private static final Logger logger = Logger.getLogger(company_managment_serivce.class.getName());

    private final ICompanyRepository companyRepository;
    // private final PolicyService policyService; // הקשר ל-PolicyService לפי התרשים

    @Autowired
    public company_managment_serivce(ICompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
        // this.policyService = policyService;
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

        // policyService.updateCompanyPolicy(companyId, newPolicyId);
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
    public void appointManager(int actingUserId, int companyId, int newManagerId, Set<CompanyPermission> permissions) {
    Company company = getCompanyOrThrow(companyId);
    if (company.getCompanyFounderId() != actingUserId) {
        throw new SecurityException("Only the founder can appoint managers.");
    }
    company.appointManager(actingUserId, newManagerId, permissions);
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

    // --- II.4.8: Appoint Additional Company Owner ---
    public void appointAdditionalOwner(int actingOwnerId, int companyId, int newOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        company.appointAdditionalOwner(actingOwnerId, newOwnerId);
        companyRepository.save(company);

        logger.info("Additional owner appointed. companyId=" + companyId +
                ", newOwnerId=" + newOwnerId +
                ", actingOwnerId=" + actingOwnerId);
    }

    // --- II.4.9: Remove Company Owner Appointment ---
    public void removeOwnerAppointment(int actingOwnerId, int companyId, int targetOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        company.removeOwnerAppointment(actingOwnerId, targetOwnerId);
        companyRepository.save(company);

        logger.info("Owner appointment removed. companyId=" + companyId +
                ", targetOwnerId=" + targetOwnerId +
                ", actingOwnerId=" + actingOwnerId);
    }

    // --- II.4.10: Resign from Ownership ---
    public void resignOwnership(int actingOwnerId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        company.resignOwnership(actingOwnerId);
        companyRepository.save(company);

        logger.info("Owner resigned from ownership. companyId=" + companyId +
                ", ownerId=" + actingOwnerId);
    }

    // --- II.4.11: Modify Manager Permissions ---
    public void modifyManagerPermissions(int actingOwnerId,
                                         int companyId,
                                         int managerId,
                                         Set<CompanyPermission> updatedPermissions) {
        Company company = getCompanyOrThrow(companyId);
        company.modifyManagerPermissions(actingOwnerId, managerId, updatedPermissions);
        companyRepository.save(company);

        logger.info("Manager permissions updated. companyId=" + companyId +
                ", managerId=" + managerId +
                ", actingOwnerId=" + actingOwnerId);
    }

    // --- II.4.12: Remove Manager Appointment ---
    public void removeManagerAppointment(int actingOwnerId, int companyId, int managerId) {
        Company company = getCompanyOrThrow(companyId);
        company.removeManagerAppointment(actingOwnerId, managerId);
        companyRepository.save(company);

        logger.info("Manager appointment removed. companyId=" + companyId +
                ", managerId=" + managerId +
                ", actingOwnerId=" + actingOwnerId);
    }

    // --- II.4.13: Suspend / Close Production Company ---
    public boolean closeCompany(int actingFounderId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        boolean changed = company.closeCompany(actingFounderId);
        companyRepository.save(company);

        logger.info("Company close requested. companyId=" + companyId +
                ", founderId=" + actingFounderId +
                ", changed=" + changed);

        return changed;
    }

    // --- II.4.14: Reopen Production Company ---
    public boolean reopenCompany(int actingFounderId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        boolean changed = company.reopenCompany(actingFounderId);
        companyRepository.save(company);

        logger.info("Company reopen requested. companyId=" + companyId +
                ", founderId=" + actingFounderId +
                ", changed=" + changed);

        return changed;
    }

    // --- II.4.15: View Roles and Permissions ---
    public CompanyRolesViewDTO viewRolesAndPermissions(int actingOwnerId, int companyId) {
        Company company = getCompanyOrThrow(companyId);

        if (!company.isOwner(actingOwnerId)) {
            throw new SecurityException("Only a company owner can view roles and permissions.");
        }

        return new CompanyRolesViewDTO(
                company.getCompanyId(),
                company.getCompanyFounderId(),
                company.getOwnerIds(),
                company.getManagerPermissionsView()
        );
    }



    // --- Internal Helpers ---

    private void validateOwnerOrManagerWithPermission(int userId, Company company, CompanyPermission permission) {
        if (company.isOwner(userId)) {
            return;
        }

        if (company.isManager(userId) && company.managerHasPermission(userId, permission)) {
            return;
        }

        throw new SecurityException("Unauthorized action for user " + userId + ".");
    }

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
