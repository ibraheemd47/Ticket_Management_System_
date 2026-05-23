package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CompanyAuthorizationDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;

@Service
public class company_managment_serivce {
    private final CompanyAuthorizationDomainService companyAuthorizationDomainService;
    private static final Logger logger = LoggerFactory.getLogger(company_managment_serivce.class);

    private final NotificationService notificationService;
    // Repositories
    private final CompanyRepository companyRepository;
    private UserRepository userRepository;
    private IEventRepository eventRepository;
    private IrepresnteUserService representUserService;

    @Autowired
    public company_managment_serivce(CompanyRepository companyRepository,
            UserRepository userRepository,
            IEventRepository eventRepository,
            IrepresnteUserService representUserService,
            NotificationService notificationService) {

        this.companyAuthorizationDomainService = new CompanyAuthorizationDomainService();
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.representUserService = representUserService;
        this.notificationService = notificationService;
    }

    // --- II.2.1: View Active Production Companies ---
    public List<CompanyDTO> getActiveCompanies() {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .map(this::toDTO)
                .toList();
    }

    // II.2.1 - Get all upcoming events from active companies
    public List<UUID> getAllUpComingEventsForHomePage() {
        // this return all of the events of all the companies, and the events service
        // should handle the date filtering and return only the upcoming events in the
        // response
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }
    

    // --- II.3.2: Open Production Company (Triggered by II.1.1) ---
    @Transactional
    public UUID openCompany(String actorToken, String name) {
        try {
            Member actor = getActorFromToken(actorToken);
            companyAuthorizationDomainService.assertCanOpenCompany(actor);

            logger.info("Opening company. founderId={}", actor.getMemberId());

            Company newCompany = new Company(name, actor.getMemberId());
            Company savedCompany = companyRepository.save(newCompany);

            actor.addCompanyRole(new CompanyRoleAssignment(
                    savedCompany.getCompanyId(),
                    actor.getMemberId(),
                    CompanyRoleType.OWNER,
                    Set.of()
            ));

            userRepository.save(actor);

            logger.info("Company opened successfully. companyId={}", savedCompany.getCompanyId());

            return savedCompany.getCompanyId();

        } catch (Exception e) {
            logger.error("Failed to open company. name={}, error={}", name, e.getMessage());
            throw e;
        }
    }

    // --- II.4.1: Manage Events (Add/Remove) ---
    @Transactional
    public EventDto addEvent(String actorToken, UUID companyId, EventDto dto) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        company.validateActionPermission(actor.getMemberId(), CompanyPermission.MANAGE_EVENTS);

        // Event entity uses Long ownerId; the rest of the system uses String
        // memberIds (UUIDs). Until Event is migrated, fall back to 0L as a
        // sentinel — the actor's authority was already verified above.
        Long ownerId = 0L;
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);

        if (dto.venue != null && !dto.venue.isBlank())
            event.editVenue(dto.venue, ownerId);

        if (dto.startDate != null || dto.endDate != null) {
            java.util.Date startDate = dto.startDate != null
                ? java.util.Date.from(dto.startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()) : null;
            java.util.Date endDate = dto.endDate != null
                ? java.util.Date.from(dto.endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()) : null;
            event.editDates(startDate, endDate, ownerId);
        }

        Event savedEvent = eventRepository.save(event);

        company.addEventId(actor.getMemberId(), savedEvent.getEventId());
        companyRepository.save(company);

        java.time.LocalDate retStart = savedEvent.getStartDate() == null ? null :
            savedEvent.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate retEnd = savedEvent.getEndDate() == null ? null :
            savedEvent.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        return new EventDto(
                savedEvent.getEventId(),
                savedEvent.getName(),
                retStart,
                retEnd,
                savedEvent.getEventType(),
                savedEvent.getVenue(),
                null);
    }

    @Transactional
    public void removeEvent(String actorToken, UUID companyId, UUID eventId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event ID " + eventId + " not found."));

        if (!Objects.equals(event.getCompanyId(), companyId)) {
            throw new IllegalArgumentException("Event does not belong to this company.");
        }

        company.removeEvent(actor.getMemberId(), eventId);
        eventRepository.delete(event);
        companyRepository.save(company);
    }

    // --- II.4.3:not for this version
    // public void updateCompanyPolicies(int actingUserId, UUID companyId, int
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
    public void respondToInquiry(String actorToken, UUID companyId, int inquiryId, String response) {
        try {
            Member actor = getActorFromToken(actorToken);
            Company company = getCompanyOrThrow(companyId);

            company.respondToInquiry(actor.getMemberId(), inquiryId, response);
            companyRepository.save(company);

            logger.info("Inquiry {} responded in company {} by user {}",
                    inquiryId, companyId, actor.getMemberId());

        } catch (Exception e) {
            logger.error("Failed to respond to inquiry. companyId={}, inquiryId={}",
                    companyId, inquiryId, e);
            throw e;
        }
    }

    // --- II.4.5: View Company Purchase and Order History ---
    public List<Integer> getPurchaseHistory(String actorToken, UUID companyId) {
        try {
            Member actor = getActorFromToken(actorToken);
            Company company = getCompanyOrThrow(companyId);

            List<Integer> history = company.getPurchaseHistoryIds(actor.getMemberId());

            logger.info("Purchase history fetched. companyId={}, user={}, records={}",
                    companyId, actor.getMemberId(), history.size());

            return history;

        } catch (Exception e) {
            logger.error("Failed to fetch purchase history. companyId={}", companyId, e);
            throw e;
        }
    }

    public List<Integer> getOrderHistory(String actorToken, UUID companyId) {
        try {
            Member actor = getActorFromToken(actorToken);
            Company company = getCompanyOrThrow(companyId);

            List<Integer> history = company.getOrderHistoryIds(actor.getMemberId());

            logger.info("Order history fetched. companyId={}, user={}, records={}",
                    companyId, actor.getMemberId(), history.size());

            return history;

        } catch (Exception e) {
            logger.error("Failed to fetch order history. companyId={}", companyId, e);
            throw e;
        }
    }

    // --- II.4.6: Reporting ---
    /** Use Case II.4.6: Generate sales report including subtree data */
    public void generateSalesReport(String actorToken, UUID companyId) {
        try {
            Member actor = getActorFromToken(actorToken);
            Company company = getCompanyOrThrow(companyId);

            company.generateSalesReport(actor.getMemberId());

            logger.info("Sales report generated. companyId={}, user={}",
                    companyId, actor.getMemberId());

        } catch (Exception e) {
            logger.error("Failed to generate sales report. companyId={}", companyId, e);
            throw e;
        }
    }

    // --- II.4.7: View and Appoint Company Managers ---
    @Transactional
    public void appointManager(String actorToken, UUID companyId, String newManagerId,
            Set<CompanyPermission> permissions) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanAssignManager(actor, company);

        company.appointManager(actor.getMemberId(), newManagerId, permissions);
        companyRepository.save(company);
        Member newManager = userRepository.findById(newManagerId)
                .orElseThrow(() -> new NoSuchElementException("New manager member not found"));

        newManager.addCompanyRole(new CompanyRoleAssignment(
                companyId,
                actor.getMemberId(),
                CompanyRoleType.MANAGER,
                Set.of()));
        userRepository.save(newManager);

        //notification: notify new manager
        notificationService.notifyManagerAppointed(newManagerId, company.getCompanyName());

        logger.info("Manager appointed successfully. companyId={}, newManagerId={}, actingOwnerId={}",
                companyId, newManagerId, actor.getMemberId());
    }

    // // --- II.4.13 & II.4.14: Set Company Status (Open/Close) ---
    // public void setCompanyStatus(int actingUserId, UUID companyId, boolean open) {
    // Company company = getCompanyOrThrow(companyId);
    // if (company.getCompanyFounderId() != actingUserId) {
    // throw new SecurityException("Only the founder can open or close the
    // company.");
    // }
    // company.setOpen(open);
    // companyRepository.save(company);
    // }

    // --- II.4.8: Appoint Additional Company Owner ---
    @Transactional
    public void appointAdditionalOwner(String actorToken, UUID companyId, String newOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanAssignOwner(actor, company);

        company.appointAdditionalOwner(actor.getMemberId(), newOwnerId);
        companyRepository.save(company);
        Member newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new NoSuchElementException("New owner member not found"));

        newOwner.addCompanyRole(new CompanyRoleAssignment(
                companyId,
                actor.getMemberId(),
                CompanyRoleType.OWNER,
                Set.of()));
        userRepository.save(newOwner);

        //notifications
        notificationService.notifyOwnerAppointed(newOwnerId, company.getCompanyName());

        logger.info("Additional owner appointed. companyId={}, newOwnerId={}, actingOwnerId={}",
                companyId, newOwnerId, actor.getMemberId());
    }

    // --- II.4.9: Remove Company Owner Appointment ---
    public void removeOwnerAppointment(String actorToken, UUID companyId, String targetOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanRemoveOwner(actor, company, targetOwnerId);

        company.removeOwnerAppointment(actor.getMemberId(), targetOwnerId);
        companyRepository.save(company);

        //notifications
        notificationService.notifyOwnerRemoved(targetOwnerId, company.getCompanyName());

        logger.info("Owner appointment removed. companyId={}, targetOwnerId={}, actingOwnerId={}",
                companyId, targetOwnerId, actor.getMemberId());
    }

    // --- II.4.10: Resign from Ownership ---
    public void resignOwnership(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        company.resignOwnership(actor.getMemberId());
        companyRepository.save(company);
    }

    // --- II.4.11: Modify Manager Permissions ---
    public void modifyManagerPermissions(String actorToken, UUID companyId, String managerId,
            Set<CompanyPermission> updatedPermissions) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanModifyManagerPermissions(actor, company, managerId);

        company.modifyManagerPermissions(actor.getMemberId(), managerId, updatedPermissions);
        companyRepository.save(company);

        //notifications
        notificationService.notifyPermissionsChanged(managerId, company.getCompanyName());

        logger.info("Manager permissions updated. companyId={}, managerId={}, actingOwnerId={}",
                companyId, managerId, actor.getMemberId());
    }

    // --- II.4.12: Remove Manager Appointment ---
    public void removeManagerAppointment(String actorToken, UUID companyId, String managerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanRemoveManager(actor, company, managerId);

        company.removeManagerAppointment(actor.getMemberId(), managerId);
        companyRepository.save(company);

        //notifications
        notificationService.notifyManagerRemoved(managerId, company.getCompanyName());

        logger.info("Manager appointment removed. companyId={}, managerId={}, actingOwnerId={}",
                companyId, managerId, actor.getMemberId());
    }

    // --- II.4.13: Suspend / Close Production Company ---
    public boolean closeCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanCloseCompany(actor, company);

        boolean changed = company.closeCompany(actor.getMemberId());
        companyRepository.save(company);

        if (changed) {
            notifyCompanyRoleMembers(company, true);
        }

        logger.info("Company close requested. companyId={}, actorId={}, changed={}",
                companyId, actor.getMemberId(), changed);

        return changed;
    }

    // --- II.4.14: Reopen Production Company ---
    public boolean reopenCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanReopenCompany(actor, company);

        boolean changed = company.reopenCompany(actor.getMemberId());
        companyRepository.save(company);

        if (changed) {
            notifyCompanyRoleMembers(company, false);
        }

        logger.info("Company reopen requested. companyId={}, actorId={}, changed={}",
                companyId, actor.getMemberId(), changed);

        return changed;
    }

    // --- II.4.15: View Roles and Permissions ---
    public CompanyRolesViewDTO viewRolesAndPermissions(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanViewRoles(actor, company);

        return new CompanyRolesViewDTO(
                company.getCompanyId(),
                company.getCompanyFounderId(),
                company.getOwnerIds(),
                company.getManagerPermissionsView());
    }


    // --- II.6.1: Close Production Company by System Admin ---
    @Transactional
    public boolean adminCloseCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanAdminCloseCompany(actor, company);

        boolean changed = company.adminCloseCompany();
        companyRepository.save(company);

        if (changed) {
            notifyCompanyRoleMembers(company, true);
        }

        logger.info("Company closed by system admin. companyId={}, adminId={}, changed={}",
                companyId, actor.getMemberId(), changed);

        return changed;
    }

    // --- Internal Helpers ---

    // private void validateOwnerOrManagerWithPermission(String userId, Company
    // company, CompanyPermission permission) {
    // if (company.isOwner(userId)) {
    // return;
    // }
    // if (company.isManager(userId) && company.managerHasPermission(userId,
    // permission)) {
    // return;
    // }
    // throw new SecurityException("Unauthorized action for user " + userId + ".");
    // }

    private Company getCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company ID " + companyId + " not found."));
    }

    // private void validateManagerOrFounder(String userId, Company company) {
    // boolean isFounder = company.getCompanyFounderId().equals(userId);
    // boolean isManager = company.getManagers().contains(userId);
    // if (!isFounder && !isManager) {
    // throw new SecurityException("Unauthorized: User " + userId + " is not a
    // manager/owner of this company.");
    // }
    // }

    // for endpoints:

    // Filter events by company name
    public List<UUID> filterEventsByCompanyName(String companyName) {
        return companyRepository.findAll().stream()
                .filter(Company::isOpen)
                .filter(company -> company.matchesName(companyName))
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }

    public List<UUID> getAllEventsByCompany(UUID companyId) {
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

    public EventDto getEventDetails(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event ID " + eventId + " not found."));

        return new EventDto(
                event.getEventId(),
                event.getName(),
                event.getStartDate().toString(),
                event.getEventType(),
                event.getVenue());
    }

    public String getCompanyLogoURL(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        return company.getLogoURL();
    }

    public String getCompanyDetails(UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        return company.getFullDetails();
    }

    public void deleteCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        if (!company.isOwner(actor.getMemberId())) {
            throw new SecurityException("Only owner can delete company");
        }

        companyRepository.deleteById(companyId);
    }


    //search company using key word
    public List<CompanyDTO> searchCompaniesByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return companyRepository.findByCompanyNameContainingIgnoreCase(keyword.trim())
                .stream()
                .filter(Company::isOpen)
                .map(this::toDTO)
                .toList();
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

    public Long getMemberIdByToken(String actorToken) {
        return Long.valueOf(getActorFromToken(actorToken).getMemberId());
    }

    // helper function
    private Member getActorFromToken(String actorToken) {
        if (actorToken == null || actorToken.isBlank()) {
            throw new SecurityException("Invalid token");
        }
        return representUserService.requireMember(actorToken);
    }


    //helper function: notify members
    private void notifyCompanyRoleMembers(Company company, boolean closed) {
        Set<String> recipients = new HashSet<>();

        recipients.add(company.getCompanyFounderId());
        recipients.addAll(company.getOwnerIds());
        recipients.addAll(company.getManagerPermissionsView().keySet());

        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }

            if (closed) {
                notificationService.notifyCompanyClosed(recipient, company.getCompanyName());
            } else {
                notificationService.notifyCompanyReopened(recipient, company.getCompanyName());
            }
        }
    }

}
