package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.sdnah.Ticket_Management_System_.Domain_Layer.CompanyAuthorizationDomainService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.ICompanyRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;

public class company_managment_serivce {
    private final CompanyAuthorizationDomainService companyAuthorizationDomainService;
    private static final Logger logger = LoggerFactory.getLogger(company_managment_serivce.class);
    private final ICompanyRepository companyRepository;
    private UserService userService;

    @Autowired
    public company_managment_serivce(ICompanyRepository companyRepository, UserService userService) {
        this.companyAuthorizationDomainService = new CompanyAuthorizationDomainService();
        this.companyRepository = companyRepository;
        this.userService = userService;
    }

    // --- II.2.1: View Active Production Companies ---
    public List<CompanyDTO> getActiveCompanies() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .map(this::toDTO)
                .toList();
    }

    // II.2.1 - Get all upcoming events from active companies
    public List<Integer> getAllUpComingEventsForHomePage() {
        // this return all of the events of all the companies, and the events service
        // should handle the date filtering and return only the upcoming events in the
        // response
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }

    // --- II.3.2: Open Production Company (Triggered by II.1.1) ---
    public void openCompany( String actorToken, int companyId, String name, int founderId) {
        // Company company = getCompanyOrThrow(companyId);
        // validateManagerOrFounder(founderId, company);
        try {
            Member actor = userService.getMemberByToken(actorToken);

            companyAuthorizationDomainService.assertCanOpenCompany(actor);
            logger.info("Opening company. companyId={}, founderId={}", companyId, founderId);
            if (companyRepository.existsById(companyId)) {
                throw new IllegalStateException("Company ID already exists.");
            }
            Company newCompany = new Company(companyId, name, founderId);
            companyRepository.save(newCompany);
            logger.info("Company opened successfully. companyId={}", companyId);
        } catch (Exception e) {
            logger.error("Failed to open company. companyId={}, error={}", companyId, e.getMessage());
            throw e;
        }
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
    // public void updateCompanyPolicies(int actingUserId, int companyId, int
    // newPolicyId) {
    // Company company = getCompanyOrThrow(companyId);

    // if (company.getCompanyFounderId() != actingUserId) {
    // throw new SecurityException("Only the founder is authorized to update company
    // policies.");
    // }

    // // policyService.updateCompanyPolicy(companyId, newPolicyId);
    // }

    // --- II.4.4: Communication ---
    /** Use Case II.4.4: Receive and respond to inquiries */
    public void respondToInquiry(int actorId, int companyId, int inquiryId, String response) {
        // Company company = getCompanyOrThrow(companyId);
        // company.respondToInquiry(actorId, inquiryId, response);
        // companyRepository.save(company);
        try {
            logger.info("Responding to inquiry. companyId={}, actorId={}, inquiryId={}",
                    companyId, actorId, inquiryId);
            Company company = getCompanyOrThrow(companyId);
            company.respondToInquiry(actorId, inquiryId, response);
            companyRepository.save(company);
            logger.info("Inquiry response saved successfully. companyId={}, inquiryId={}",
                    companyId, inquiryId);
        } catch (Exception e) {
            logger.error("Failed to respond to inquiry. companyId={}, actorId={}, inquiryId={}",
                    companyId, actorId, inquiryId, e);
            throw e;
        }
    }

    // --- II.4.5: View Company Purchase and Order History ---
    public List<Integer> getPurchaseHistory(int actingUserId, int companyId) {
        // Company company = getCompanyOrThrow(companyId);
        // validateManagerOrFounder(actingUserId, company);
        // return company.getPurchaseHistoryIds(actingUserId);
        try {
            logger.info("Fetching purchase history. companyId={}, actingUserId={}",
                    companyId, actingUserId);
            Company company = getCompanyOrThrow(companyId);
            validateManagerOrFounder(actingUserId, company);
            List<Integer> history = company.getPurchaseHistoryIds(actingUserId);
            logger.info("Purchase history fetched successfully. companyId={}, records={}",
                    companyId, history.size());
            return history;
        } catch (Exception e) {
            logger.error("Failed to fetch purchase history. companyId={}, actingUserId={}",
                    companyId, actingUserId, e);
            throw e;
        }
    }

    public List<Integer> getOrderHistory(int actingUserId, int companyId) {
        // Company company = getCompanyOrThrow(companyId);
        // validateManagerOrFounder(actingUserId, company);
        // return company.getOrderHistoryIds(actingUserId);
        try {
            logger.info("Fetching order history. companyId={}, actingUserId={}",
                    companyId, actingUserId);
            Company company = getCompanyOrThrow(companyId);
            validateManagerOrFounder(actingUserId, company);
            List<Integer> history = company.getOrderHistoryIds(actingUserId);
            logger.info("Order history fetched successfully. companyId={}, records={}",
                    companyId, history.size());
            return history;
        } catch (Exception e) {
            logger.error("Failed to fetch order history. companyId={}, actingUserId={}",
                    companyId, actingUserId, e);
            throw e;
        }
    }

    // --- II.4.6: Reporting ---
    /** Use Case II.4.6: Generate sales report including subtree data */
    public void generateSalesReport(int actorId, int companyId) {
        // Company company = getCompanyOrThrow(companyId);
        // validateManagerOrFounder(actorId, company);
        // company.generateSalesReport(actorId);
        try {
            logger.info("Generating sales report. companyId={}, actorId={}", companyId, actorId);
            Company company = getCompanyOrThrow(companyId);
            validateManagerOrFounder(actorId, company);
            company.generateSalesReport(actorId);
            logger.info("Sales report generated successfully. companyId={}, actorId={}",
                    companyId, actorId);
        } catch (Exception e) {
            logger.error("Failed to generate sales report. companyId={}, actorId={}",
                    companyId, actorId, e);
            throw e;
        }
    }

    // --- II.4.7: View and Appoint Company Managers ---
    public void appointManager(int actingUserId, int companyId, int newManagerId, Set<CompanyPermission> permissions) {
        // Company company = getCompanyOrThrow(companyId);
        // if (company.getCompanyFounderId() != actingUserId) {
        // throw new SecurityException("Only the founder can appoint managers.");
        // }
        // company.appointManager(actingUserId, newManagerId, permissions);
        // companyRepository.save(company);
        try {
            logger.info("Appointing manager. companyId={}, actingUserId={}, newManagerId={}",
                    companyId, actingUserId, newManagerId);
            Company company = getCompanyOrThrow(companyId);
            if (company.getCompanyFounderId() != actingUserId) {
                throw new SecurityException("Only the founder can appoint managers.");
            }
            company.appointManager(actingUserId, newManagerId, permissions);
            companyRepository.save(company);
            logger.info("Manager appointed successfully. companyId={}, newManagerId={}",
                    companyId, newManagerId);
        } catch (Exception e) {
            logger.error("Failed to appoint manager. companyId={}, actingUserId={}, newManagerId={}",
                    companyId, actingUserId, newManagerId, e);
            throw e;
        }
    }

    // // --- II.4.13 & II.4.14: Set Company Status (Open/Close) ---
    // public void setCompanyStatus(int actingUserId, int companyId, boolean open) {
    // Company company = getCompanyOrThrow(companyId);
    // if (company.getCompanyFounderId() != actingUserId) {
    // throw new SecurityException("Only the founder can open or close the
    // company.");
    // }
    // company.setOpen(open);
    // companyRepository.save(company);
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
    public void modifyManagerPermissions(int actingOwnerId, int companyId, int managerId,
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
                company.getManagerPermissionsView());
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

    // for endpoints:

    // Filter events by company name
    public List<Integer> filterEventsByCompanyName(String companyName) {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .filter(company -> company.matchesName(companyName))
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }

    public List<Integer> getAllEventsByCompany(int companyId) {
        Company company = getCompanyOrThrow(companyId);
        return company.getAssociatedEventIds();
    }

    public List<CompanyDTO> showCompaniesByRating0() {
        throw new UnsupportedOperationException("Company rating is not implemented yet.");
    }

    public List<CompanyDTO> showCompaniesByRating() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .sorted(Comparator.comparingDouble(Company::getRating).reversed())
                .map(this::toDTO)
                .toList();
    }

    public List<Company> searchByCompanyName(String companyName) {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .filter(company -> company.matchesName(companyName))
                .toList();
    }

    public int getEventDetails(int eventId) {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .filter(company -> company.hasEvent(eventId))
                .findFirst()
                .map(company -> eventId)
                .orElseThrow(() -> new NoSuchElementException("Event ID " + eventId + " not found."));
    }

    public String getCompanyLogoURL(int companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        return company.getLogoURL();
    }

    public String getCompanyDetails(int companyId) {
        Company company = getCompanyOrThrow(companyId);
        return company.getFullDetails();
    }

    public void deleteCompany(int userId, int companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found"));

        if (!company.isOwner(userId)) {
            throw new SecurityException("Only owner can delete company");
        }

        companyRepository.deleteById(companyId);
    }

    // helper for dtos
    private CompanyDTO toDTO(Company company) {
        return new CompanyDTO(
                company.getCompanyId(),
                company.getCompanyName(),
                company.isOpen(),
                company.getRating(),
                company.getLogoURL());
    }

}
