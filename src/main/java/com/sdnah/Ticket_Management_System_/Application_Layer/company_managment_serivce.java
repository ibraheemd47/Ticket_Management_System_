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

    @Autowired
    public company_managment_serivce(ICompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // --- II.2.1: View Active Production Companies ---
    public List<Company> getActiveCompanies() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .toList();
    }

    // II.2.1 - Get all upcoming events from active companies
    public List<Integer> getAllUpComingEventsForHomePage() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }


    // --- II.3.2: Open Production Company (Triggered by II.1.1) ---
    public void openCompany(int companyId, String name, int founderId) {
        //Company company = getCompanyOrThrow(companyId);
        //validateManagerOrFounder(founderId, company);
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
        company.addEventId(actingUserId, eventId);
        companyRepository.save(company);
        logger.info("Event " + eventId + " added to company " + companyId);
    }

    public void removeEvent(int actingUserId, int companyId, int eventId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        company.removeEvent(eventId);
        companyRepository.save(company);
        logger.info("Event " + eventId + " removed from company " + companyId);
    }

    // --- II.4.3:not for this version
    // public void updateCompanyPolicies(int actingUserId, int companyId, int newPolicyId) {
    //     Company company = getCompanyOrThrow(companyId);
        
    //     if (company.getCompanyFounderId() != actingUserId) {
    //         throw new SecurityException("Only the founder is authorized to update company policies.");
    //     }

    //     // policyService.updateCompanyPolicy(companyId, newPolicyId);
    // }

    // --- II.4.4: Communication ---
    /** Use Case II.4.4: Receive and respond to inquiries */
    public void respondToInquiry(int actorId, int companyId, int inquiryId, String response) {
        Company company = getCompanyOrThrow(companyId);
        company.respondToInquiry(actorId, inquiryId, response);
        companyRepository.save(company);
    }


    // --- II.4.5: View Company Purchase and Order History ---
    public List<Integer> getPurchaseHistory(int actingUserId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        return company.getPurchaseHistoryIds(actingUserId);
    }

    public List<Integer> getOrderHistory(int actingUserId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actingUserId, company);
        return company.getOrderHistoryIds(actingUserId);
    }

    // --- II.4.6: Reporting ---
    /** Use Case II.4.6: Generate sales report including subtree data */
    public void generateSalesReport(int actorId, int companyId) {
        Company company = getCompanyOrThrow(companyId);
        validateManagerOrFounder(actorId, company);
        company.generateSalesReport(actorId);
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
    

        //for endpoints:

        // Filter events by company name
        public List<Integer> filterEventsByCompanyName(String companyName) {
            return companyRepository.findAll().stream()
                    .filter(Company::isOpen)
                    .filter(company -> company.matchesName(companyName))
                    .flatMap(company -> company.getAssociatedEventIds().stream())
                    .toList();
        }

        public List<Company> showCompaniesByRating0() 
        {
            throw new UnsupportedOperationException("Company rating is not implemented yet.");
        }

        public List<Company> showCompaniesByRating() {
            return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .sorted(Comparator.comparingDouble(Company::getRating).reversed())
                .toList();
        }

        public List<Company> searchByCompanyName(String companyName) 
        {
            return companyRepository.findAll().stream()
                    .filter(Company::isOpen)
                    .filter(company -> company.matchesName(companyName))
                    .toList();
        }

    public int getEventDetails(int eventId) 
    {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .filter(company -> company.hasEvent(eventId))
                .findFirst()
                .map(company -> eventId)
                .orElseThrow(() -> new NoSuchElementException("Event ID " + eventId + " not found."));
    }

}
