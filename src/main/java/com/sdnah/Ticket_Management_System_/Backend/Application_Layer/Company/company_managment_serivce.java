package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.OwnerAppointmentRequestDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.PurchaseHistoryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.SalesReportDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CompanyAuthorizationDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.userOrderDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.OwnerAppointmentRequest;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.OwnerAppointmentRequestRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

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
    private PolicyRepository policyRepo;
    private final PurchaseRepository purchaseRepository;
    private final OwnerAppointmentRequestRepository ownerRequestRepository;

    @Autowired
    public company_managment_serivce(CompanyRepository companyRepository,
            UserRepository userRepository,
            IEventRepository eventRepository,
            IrepresnteUserService representUserService,
            NotificationService notificationService, PolicyRepository policyRepo,
            PurchaseRepository purchaseRepository,
            OwnerAppointmentRequestRepository ownerRequestRepository) {

        this.companyAuthorizationDomainService = new CompanyAuthorizationDomainService();
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.representUserService = representUserService;
        this.notificationService = notificationService;
        this.policyRepo = policyRepo;
        this.purchaseRepository = purchaseRepository;
        this.ownerRequestRepository = ownerRequestRepository;
    }

    /** 7-arg overload — wires the new owner-request flow into a null repo. */
    public company_managment_serivce(CompanyRepository companyRepository,
            UserRepository userRepository,
            IEventRepository eventRepository,
            IrepresnteUserService representUserService,
            NotificationService notificationService, PolicyRepository policyRepo,
            PurchaseRepository purchaseRepository) {
        this(companyRepository, userRepository, eventRepository, representUserService,
                notificationService, policyRepo, purchaseRepository, null);
    }

    /**
     * Legacy 6-arg constructor — keeps existing tests compiling. Sales
     * report aggregation (II.4.6) needs a PurchaseRepository, so callers
     * that don't supply one get a null and the report method will NPE if
     * exercised. Production wiring uses the 7-arg constructor via Spring.
     */
    public company_managment_serivce(CompanyRepository companyRepository,
            UserRepository userRepository,
            IEventRepository eventRepository,
            IrepresnteUserService representUserService,
            NotificationService notificationService, PolicyRepository policyRepo) {
        this(companyRepository, userRepository, eventRepository, representUserService,
                notificationService, policyRepo, null);
    }
    public Company getCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + companyId));
    }


    // --- II.2.1: View Active Production Companies ---
    @Cacheable("active-companies")
    public List<CompanyDTO> getActiveCompanies() {
        return companyRepository.findByIsOpen(true).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Lookup a single company DTO regardless of its open/closed status.
     * Used by the "my companies" chooser so closed companies still
     * resolve to their name (otherwise the row renders nameless and the
     * user can't tell what got closed).
     */
    public java.util.Optional<CompanyDTO> getCompanyById(UUID companyId) {
        return companyRepository.findById(companyId).map(this::toDTO);
    }

    /**
     * Drop every {@link CompanyRoleAssignment} on this member that no
     * longer matches the company's current roster (owner or manager).
     * The chooser calls this on each page load so a previously-removed
     * user (e.g. removed before the role-sync fix landed) stops seeing
     * the company in their "My companies" list.
     */
    @Transactional
    public void reconcileMyCompanyRoles(String actorToken) {
        Member actor = getActorFromToken(actorToken);
        java.util.Set<UUID> stale = new HashSet<>();

        for (var role : actor.getCompanyRoles()) {
            UUID cid = role.getCompanyId();
            // Company gone entirely → stale.
            var maybe = companyRepository.findById(cid);
            if (maybe.isEmpty()) { stale.add(cid); continue; }
            Company company = maybe.get();
            boolean stillOnRoster = company.isOwner(actor.getMemberId())
                    || company.isManager(actor.getMemberId())
                    || actor.getMemberId().equals(company.getCompanyFounderId());
            if (!stillOnRoster) stale.add(cid);
        }
        if (stale.isEmpty()) return;

        boolean changed = false;
        for (UUID cid : stale) {
            changed |= actor.removeCompanyRoles(cid);
        }
        if (changed) userRepository.save(actor);
    }

    // II.2.1 - Get all upcoming events from active companies
    public List<UUID> getAllUpComingEventsForHomePage() {
        return companyRepository.findByIsOpen(true).stream()
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }
    

    // --- II.3.2: Open Production Company (Triggered by II.1.1) ---
    @CacheEvict(value = "active-companies", allEntries = true)
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

            //policy
            // Create default company-level policies
            DiscountPolicy discountPolicy =
                new DiscountPolicy(
                    
                    "Default company discount policy",
                    null,
                    savedCompany.getCompanyId()
                );

            PurchasePolicy purchasePolicy =
                new PurchasePolicy(
                    
                    "Default company purchase policy",
                    null,
                    savedCompany.getCompanyId()
                );

            SellingPolicy sellingPolicy =
                new SellingPolicy(
                        
                        "Default company selling policy",
                        SellingPolicy.SellingType.REGULAR,
                        null,
                        savedCompany.getCompanyId());

            policyRepo.savePolicy(discountPolicy);
            policyRepo.savePolicy(purchasePolicy);
            policyRepo.savePolicy(sellingPolicy);

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

        String ownerId = actor.getMemberId();
        Event event = new Event(dto.name, dto.eventType, companyId, ownerId);

        if (dto.venue != null && !dto.venue.isBlank())
            event.editVenue(dto.venue, ownerId);

        if (dto.startDate != null || dto.endDate != null) {
            java.util.Date startDate = dto.startDate != null
                ? java.util.Date.from(dto.startDate.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;
            java.util.Date endDate = dto.endDate != null
                ? java.util.Date.from(dto.endDate.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;
            event.editDates(startDate, endDate, ownerId);
        }

        Event savedEvent = eventRepository.save(event);

        company.addEventId(actor.getMemberId(), savedEvent.getEventId());
        companyRepository.save(company);

        java.time.LocalDateTime retStart = savedEvent.getStartDate() == null ? null :
            savedEvent.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDateTime retEnd = savedEvent.getEndDate() == null ? null :
            savedEvent.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

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
    @Transactional
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
    @Transactional(readOnly = true)
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

    /**
     * II.4.5 — rich purchase history for a company's events. One row per
     * {@code Purchase} across every event the company owns. Honours the
     * same VIEW_HISTORY authorization gate as the legacy id-only variant
     * via {@code company.getPurchaseHistoryIds}.
     *
     * <p>{@code @Transactional} is required because the auth call dips into
     * {@code Company.purchaseHistoryIds}, a lazy {@code @OneToMany}; without
     * an open Hibernate session that access throws
     * {@link org.hibernate.LazyInitializationException}.
     */
    @Transactional(readOnly = true)
    public List<PurchaseHistoryEntryDTO> getCompanyPurchaseHistory(String actorToken, UUID companyId) {
        Member actor = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);
        // Auth: throws if actor lacks VIEW_HISTORY for this company.
        company.getPurchaseHistoryIds(actor.getMemberId());

        List<Event> events = eventRepository.findByCompanyId(companyId);
        java.util.Map<UUID, String> eventNames = new java.util.HashMap<>();
        for (Event ev : events) {
            eventNames.put(ev.getEventId(), ev.getName());
        }

        List<PurchaseHistoryEntryDTO> rows = new java.util.ArrayList<>();
        for (Event ev : events) {
            var purchases = purchaseRepository.findByEventId(ev.getEventId());
            for (var p : purchases) {
                rows.add(new PurchaseHistoryEntryDTO(
                        p.getPurchaseId(),
                        p.getOrderId(),
                        p.getEventId(),
                        eventNames.getOrDefault(p.getEventId(), "—"),
                        p.getbuyerId(),
                        p.getItems() == null ? 0 : p.getItems().size(),
                        p.getTotalPrice(),
                        p.getPurchasedAt()));
            }
        }
        rows.sort(java.util.Comparator.comparing(
                PurchaseHistoryEntryDTO::getPurchasedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return rows;
    }

    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

    /**
     * II.4.6 — return a structured sales report so the UI can render
     * per-event revenue plus the company-level totals.
     * Authorization reuses the same gate as the void variant above.
     *
     * <p>Backwards-compat overload — defaults to "subtree" mode, which
     * for the founder/full-permission viewer is equivalent to all events
     * of the company.
     */
    @Transactional(readOnly = true)
    public SalesReportDTO getSalesReport(String actorToken, UUID companyId) {
        return getSalesReport(actorToken, companyId, true);
    }

    /**
     * II.4.6 — scoped sales report. When {@code includeSubtree} is false
     * the result only contains events the actor manages directly
     * ({@code event.ownerId == actorId} or actor in {@code managerIds}).
     * When true the scope expands to every event whose owner or any
     * manager is in the actor's appointment sub-tree (i.e. anyone the
     * actor — transitively — appointed).
     */
    @Transactional(readOnly = true)
    public SalesReportDTO getSalesReport(String actorToken, UUID companyId, boolean includeSubtree) {
        Member actor = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);
        // Re-uses existing II.4.6 auth path; throws if actor can't view reports.
        company.generateSalesReport(actor.getMemberId());

        Set<String> scope = includeSubtree
                ? company.getAppointmentSubtree(actor.getMemberId())
                : Set.of(actor.getMemberId());

        List<Event> events = eventRepository.findByCompanyId(companyId);

        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        int totalOrders = 0;
        List<SalesReportDTO.EventLine> lines = new java.util.ArrayList<>();

        for (Event ev : events) {
            if (!eventInScope(ev, scope)) continue;

            var purchases = purchaseRepository.findByEventId(ev.getEventId());
            java.math.BigDecimal eventRevenue = purchases.stream()
                    .map(p -> p.getTotalPrice() == null ? java.math.BigDecimal.ZERO : p.getTotalPrice())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            int eventOrders = purchases.size();

            totalRevenue = totalRevenue.add(eventRevenue);
            totalOrders += eventOrders;

            lines.add(new SalesReportDTO.EventLine(
                    ev.getEventId(), ev.getName(), eventOrders, eventRevenue));
        }

        return new SalesReportDTO(
                company.getCompanyId(),
                company.getCompanyName(),
                totalOrders,
                totalRevenue,
                lines);
    }

    /**
     * Returns true if any member in {@code scope} is the event's owner
     * or appears in its manager list. An event with no owner / managers
     * is treated as out-of-scope.
     */
    private static boolean eventInScope(Event ev, Set<String> scope) {
        if (ev.getOwnerId() != null && scope.contains(ev.getOwnerId())) return true;
        List<String> mgrs = ev.getManagerIds();
        if (mgrs == null) return false;
        for (String m : mgrs) if (scope.contains(m)) return true;
        return false;
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
    /**
     * Direct (no-approval) owner appointment. Kept for tests and the legacy
     * code path that bootstraps the founder; the production UI now goes
     * through {@link #requestOwnerAppointment} so the candidate has to
     * accept first.
     */
    @Transactional
    public void appointAdditionalOwner(String actorToken, UUID companyId, String newOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanAssignOwner(actor, company);

        appointAdditionalOwnerInternal(company, actor.getMemberId(), newOwnerId);

        //notifications
        notificationService.notifyOwnerAppointed(newOwnerId, company.getCompanyName());

        logger.info("Additional owner appointed. companyId={}, newOwnerId={}, actingOwnerId={}",
                companyId, newOwnerId, actor.getMemberId());
    }

    /**
     * Apply an owner appointment to the domain + role tables. Shared by the
     * direct path and the request/accept path so behaviour stays identical
     * once approval has been granted.
     */
    private void appointAdditionalOwnerInternal(Company company,
                                                String actingOwnerId,
                                                String newOwnerId) {
        company.appointAdditionalOwner(actingOwnerId, newOwnerId);
        companyRepository.save(company);
        Member newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new NoSuchElementException("New owner member not found"));

        newOwner.addCompanyRole(new CompanyRoleAssignment(
                company.getCompanyId(),
                actingOwnerId,
                CompanyRoleType.OWNER,
                Set.of()));
        userRepository.save(newOwner);
    }

    /**
     * II.4.8 (request flow) — create a pending owner-appointment request
     * and notify the candidate. The candidate must
     * {@link #respondToOwnerAppointment respond} before they actually
     * become an owner.
     */
    @Transactional
    public UUID requestOwnerAppointment(String actorToken, UUID companyId, String candidateId) {
        if (ownerRequestRepository == null)
            throw new IllegalStateException("Owner request flow is not wired (legacy constructor)");
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        // Same authorization gate as a direct appointment.
        companyAuthorizationDomainService.assertCanAssignOwner(actor, company);

        String trimmed = candidateId == null ? "" : candidateId.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("candidate required");
        if (company.isOwner(trimmed))
            throw new IllegalArgumentException("User is already an owner of this company.");

        // Block re-requesting an already-pending candidate.
        if (ownerRequestRepository.findByCompanyIdAndCandidateIdAndStatus(
                companyId, trimmed, OwnerAppointmentRequest.Status.PENDING).isPresent()) {
            throw new IllegalStateException("There's already a pending invite for this user.");
        }

        // Candidate must exist as a Member before we can invite them.
        userRepository.findById(trimmed)
                .orElseThrow(() -> new NoSuchElementException("Candidate member not found: " + trimmed));

        OwnerAppointmentRequest req = new OwnerAppointmentRequest(
                companyId, trimmed, actor.getMemberId());
        ownerRequestRepository.save(req);

        // Display the appointer's username in the notification — the raw
        // member id is meaningless to the candidate reading the bell.
        notificationService.notifyOwnerAppointmentRequested(
                trimmed, company.getCompanyName(),
                actor.getUsername() == null ? actor.getMemberId() : actor.getUsername());

        logger.info("Owner appointment requested. companyId={}, candidateId={}, by={}",
                companyId, trimmed, actor.getMemberId());
        return req.getId();
    }

    /**
     * II.4.8 (request flow) — candidate accepts or rejects a pending
     * owner-appointment request. On accept, the same domain wiring as a
     * direct appointment is applied. The original appointer is notified
     * either way.
     */
    @Transactional
    public void respondToOwnerAppointment(String actorToken, UUID requestId, boolean accept) {
        if (ownerRequestRepository == null)
            throw new IllegalStateException("Owner request flow is not wired (legacy constructor)");
        Member actor = getActorFromToken(actorToken);
        OwnerAppointmentRequest req = ownerRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request not found: " + requestId));

        if (!actor.getMemberId().equals(req.getCandidateId()))
            throw new SecurityException("Only the invited candidate can respond to this request.");

        Company company = getCompanyOrThrow(req.getCompanyId());

        if (accept) {
            req.accept();
            appointAdditionalOwnerInternal(company, req.getAppointerId(), req.getCandidateId());
            notificationService.notifyOwnerAppointed(req.getCandidateId(), company.getCompanyName());
        } else {
            req.reject();
        }
        ownerRequestRepository.save(req);

        String candidateDisplay = actor.getUsername() == null
                ? req.getCandidateId() : actor.getUsername();
        notificationService.notifyOwnerAppointmentResponded(
                req.getAppointerId(), company.getCompanyName(), candidateDisplay, accept);

        logger.info("Owner appointment {} for request {} by candidate {}",
                accept ? "ACCEPTED" : "REJECTED", requestId, actor.getMemberId());
    }

    /** Pending invites the current member can act on (II.4.8). */
    @Transactional(readOnly = true)
    public List<OwnerAppointmentRequestDTO> getPendingOwnerInvites(String actorToken) {
        if (ownerRequestRepository == null) return List.of();
        Member actor = getActorFromToken(actorToken);
        List<OwnerAppointmentRequest> pending = ownerRequestRepository
                .findByCandidateIdAndStatus(actor.getMemberId(),
                        OwnerAppointmentRequest.Status.PENDING);
        List<OwnerAppointmentRequestDTO> out = new java.util.ArrayList<>();
        for (OwnerAppointmentRequest r : pending) {
            String companyName = companyRepository.findById(r.getCompanyId())
                    .map(Company::getCompanyName).orElse("(unknown company)");
            String appointerName = userRepository.findById(r.getAppointerId())
                    .map(Member::getUsername).orElse(r.getAppointerId());
            out.add(new OwnerAppointmentRequestDTO(
                    r.getId(), r.getCompanyId(), companyName,
                    r.getAppointerId(), appointerName, r.getCreatedAt()));
        }
        return out;
    }

    // --- II.4.9: Remove Company Owner Appointment ---
    @Transactional
    public void removeOwnerAppointment(String actorToken, UUID companyId, String targetOwnerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanRemoveOwner(actor, company, targetOwnerId);

        company.removeOwnerAppointment(actor.getMemberId(), targetOwnerId);
        companyRepository.save(company);

        userRepository.findById(targetOwnerId).ifPresent(target -> {
            target.removeCompanyRoles(companyId);
            userRepository.save(target);
        });
        // Also drop the role assignment off the target Member, otherwise
        // they still see the company in their "My companies" list and
        // any check that consults Member.companyRoles still treats them
        // as an owner.
        dropMemberCompanyRoles(targetOwnerId, companyId);

        //notifications
        notificationService.notifyOwnerRemoved(targetOwnerId, company.getCompanyName());

        logger.info("Owner appointment removed. companyId={}, targetOwnerId={}, actingOwnerId={}",
                companyId, targetOwnerId, actor.getMemberId());
    }

    // --- II.4.10: Resign from Ownership ---
    @Transactional
    public void resignOwnership(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        company.resignOwnership(actor.getMemberId());
        companyRepository.save(company);

        dropMemberCompanyRoles(actor.getMemberId(), companyId);
    }

    /**
     * Removes every {@code CompanyRoleAssignment} this member holds for
     * the given company. Mirrors the {@code addCompanyRole} done on the
     * appointment paths so the user-side role table stays in sync with
     * the company-side roles after a removal/resign.
     */
    private void dropMemberCompanyRoles(String memberId, UUID companyId) {
        if (memberId == null || memberId.isBlank()) return;
        userRepository.findById(memberId).ifPresent(m -> {
            if (m.removeCompanyRoles(companyId)) {
                userRepository.save(m);
            }
        });
    }

    // --- II.4.11: Modify Manager Permissions ---
    @Transactional
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
    @Transactional
    public void removeManagerAppointment(String actorToken, UUID companyId, String managerId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanRemoveManager(actor, company, managerId);

        company.removeManagerAppointment(actor.getMemberId(), managerId);
        companyRepository.save(company);

        // Same sync as for owners — drop the role on the Member side.
        dropMemberCompanyRoles(managerId, companyId);

        //notifications
        notificationService.notifyManagerRemoved(managerId, company.getCompanyName());

        logger.info("Manager appointment removed. companyId={}, managerId={}, actingOwnerId={}",
                companyId, managerId, actor.getMemberId());
    }

    // --- II.4.13: Suspend / Close Production Company ---
    @CacheEvict(value = "active-companies", allEntries = true)
    @Transactional
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
    @CacheEvict(value = "active-companies", allEntries = true)
    @Transactional
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
    @Transactional(readOnly = true)
    public CompanyRolesViewDTO viewRolesAndPermissions(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanViewRoles(actor, company);

        return new CompanyRolesViewDTO(
                company.getCompanyId(),
                company.getCompanyFounderId(),
                company.getOwnerIds(),
                company.getManagerPermissionsView(),
                company.getManagerAppointedByView(),
                company.getOwnerAppointedByView());
    }


    // --- II.6.1: Close Production Company by System Admin ---
    @CacheEvict(value = "active-companies", allEntries = true)
    @Transactional
    public boolean adminCloseCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        companyAuthorizationDomainService.assertCanAdminCloseCompany(actor, company);

        // boolean changed = company.adminCloseCompany();
        // companyRepository.save(company);

        // if (changed) {
        //     notifyCompanyRoleMembers(company, true);
        // }
        Set<String> recipientsBefore = new HashSet<>();
        recipientsBefore.add(company.getCompanyFounderId());
        recipientsBefore.addAll(company.getOwnerIds());
        recipientsBefore.addAll(company.getManagerPermissionsView().keySet());
 
        boolean changed = company.adminCloseCompany();
         companyRepository.save(company);

        if (changed) {
        for (String recipient : recipientsBefore) {
            if (recipient == null || recipient.isBlank()) continue;
            notificationService.notifyCompanyClosed(recipient, company.getCompanyName());
        }
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
        if (companyName == null || companyName.isBlank()) return List.of();
        return companyRepository.findActiveByNameContaining(companyName, true).stream()
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .toList();
    }

    public List<com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto> getAllEventsByCompany(UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        List<com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto> result = new java.util.ArrayList<>();
        for (UUID eid : company.getAssociatedEventIds()) {
            eventRepository.findById(eid).ifPresent(ev -> {
                java.time.LocalDateTime start = ev.getStartDate() != null
                        ? ev.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
                java.time.LocalDateTime end = ev.getEndDate() != null
                        ? ev.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
                result.add(new com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto(
                        ev.getEventId(), ev.getName(), start, end, ev.getEventType(), ev.getVenue(), ev.getPhotoUrl()));
            });
        }
        return result;
    }

    public List<CompanyDTO> showCompaniesByRating0() {
        throw new UnsupportedOperationException("Company rating is not implemented yet.");
    }

    public List<CompanyDTO> showCompaniesByRating() {
        return companyRepository.findByIsOpen(true).stream()
                .sorted(Comparator.comparingDouble(Company::getRating).reversed())
                .map(this::toDTO)
                .toList();
    }

    // public List<Company> searchByCompanyName(String companyName) {
    //     return companyRepository.findAll().stream()
    //             .filter(Company::isOpen)
    //             .filter(company -> company.matchesName(companyName))
    //             .toList();
    // }

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

    // ── Company ratings (per-user reviews, average kept on Company.rating) ──────

    /**
     * Records a member's 1–5 review of a company. The company's aggregate rating
     * is recomputed from all reviews. Evicts the active-companies cache so the new
     * rating shows up in listings / "browse by rating".
     */
    @CacheEvict(value = "active-companies", allEntries = true)
    @Transactional
    public void addReviewToCompany(UUID companyId, UUID userId, int rating) {
        Company company = getCompanyOrThrow(companyId);
        company.addReview(userId, rating);
        companyRepository.save(company);
        logger.info("Review added to company {} by user {} (rating={})", companyId, userId, rating);
    }

    /** All per-user reviews for a company, keyed by user id. */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> getCompanyReviews(UUID companyId) {
        return new java.util.HashMap<>(getCompanyOrThrow(companyId).getReviews());
    }

    /** Current aggregate (average) rating for a company. */
    public double getCompanyRating(UUID companyId) {
        return getCompanyOrThrow(companyId).getRating();
    }

    /** Display name for a company, used by views that only hold the company id. */
    public String getCompanyName(UUID companyId) {
        return getCompanyOrThrow(companyId).getCompanyName();
    }

    /** Whether a company is currently open (vs suspended/closed) — II.4.13/4.14. */
    public boolean isCompanyOpen(UUID companyId) {
        return getCompanyOrThrow(companyId).isOpen();
    }

    @CacheEvict(value = "active-companies", allEntries = true)
    @Transactional
    public void deleteCompany(String actorToken, UUID companyId) {
        Company company = getCompanyOrThrow(companyId);
        Member actor = getActorFromToken(actorToken);

        if (!company.isOwner(actor.getMemberId())) {
            throw new SecurityException("Only owner can delete company");
        }

        // Delete the company's events too — works whether or not the company has
        // events, and avoids leaving orphaned events behind in search (item 12).
        for (UUID eventId : company.getAssociatedEventIds()) {
            eventRepository.findById(eventId).ifPresent(eventRepository::delete);
        }

        // Strip this company's role assignments from every member so the deleted
        // company stops appearing in their "my companies" list.
        for (Member member : userRepository.findAll()) {
            Set<CompanyRoleAssignment> roles = member.getCompanyRoles();
            boolean hasRole = roles.stream()
                    .anyMatch(r -> companyId.equals(r.getCompanyId()));
            if (hasRole) {
                Set<CompanyRoleAssignment> kept = new HashSet<>();
                for (CompanyRoleAssignment r : roles) {
                    if (!companyId.equals(r.getCompanyId())) {
                        kept.add(r);
                    }
                }
                member.setCompanyRoles(kept);
                userRepository.save(member);
            }
        }

        companyRepository.deleteById(companyId);

        logger.info("Company deleted. companyId={}, actorId={}", companyId, actor.getMemberId());
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


    // ─────────── Company-scoped searches (UC 3b) ───────────

    //BY DESCRIPTION
    public List<EventDto> searchEventsInCompanyByDescription(String companyName, String description) {
        if (description == null || description.isBlank()) return List.of();
        String d = description.toLowerCase().trim();
        return eventsOfCompanyByName(companyName)
                .filter(e -> e.getDescription() != null && e.getDescription().toLowerCase().contains(d))
                .map(this::toEventDto)
                .toList();
    }


    //BY KEYWORD (name or description)
    public List<EventDto> searchEventsInCompanyByKeyword(String companyName, String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String kw = keyword.toLowerCase().trim();
        return eventsOfCompanyByName(companyName)
                .filter(e -> (e.getName() != null && e.getName().toLowerCase().contains(kw))
                        || (e.getDescription() != null && e.getDescription().toLowerCase().contains(kw)))
                .map(this::toEventDto)
                .toList();
    }

    //BY DATE RANGE
    public List<EventDto> searchEventsInCompanyByDateRange(String companyName, Date fromDate, Date toDate) {
        return eventsOfCompanyByName(companyName)
                .filter(e -> e.getStartDate() != null)
                .filter(e -> fromDate == null || !e.getStartDate().before(fromDate))
                .filter(e -> toDate   == null || !e.getStartDate().after(toDate))
                .map(this::toEventDto)
                .toList();
    }

    //BY CATEGORY
    public List<EventDto> searchEventsInCompanyByCategory(String companyName, show_type category) {
        if (category == null) return List.of();
        return eventsOfCompanyByName(companyName)
                .filter(e -> category.equals(e.getEventType()))
                .map(this::toEventDto)
                .toList();
    }

    //BY START DATE
    public List<EventDto> searchEventsInCompanyByStartDate(String companyName, Date startDate) {
        if (startDate == null) return List.of();
        return eventsOfCompanyByName(companyName)
                .filter(e -> e.getStartDate() != null && !e.getStartDate().before(startDate))
                .map(this::toEventDto)
                .toList();
    }

    //BY END DATE
    public List<EventDto> searchEventsInCompanyByEndDate(String companyName, Date endDate) {
        if (endDate == null) return List.of();
        return eventsOfCompanyByName(companyName)
                .filter(e -> e.getEndDate() != null && !e.getEndDate().after(endDate))
                .map(this::toEventDto)
                .toList();
    }

    // BY VENUE
    public List<EventDto> searchEventsInCompanyByVenue(String companyName, String venue) {
        if (venue == null || venue.isBlank()) return List.of();
        String v = venue.toLowerCase().trim();
        return eventsOfCompanyByName(companyName)
                .filter(e -> e.getVenue() != null && e.getVenue().toLowerCase().contains(v))
                .map(this::toEventDto)
                .toList();
    }

    // BY RATING
    public List<EventDto> searchEventsInCompanyByMinRating(String companyName, double minRating) {
        return eventsOfCompanyByName(companyName)
                .filter(e -> averageEventRating(e) >= minRating)
                .map(this::toEventDto)
                .toList();
    }

    // BY company name only (no additional filters)
    public List<EventDto> getAllEventsByCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) return List.of();
        return companyRepository.findActiveByNameContaining(companyName, true).stream()
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .map(id -> eventRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toEventDto)
                .toList();
    }

    


    // ─────────── private helpers ───────────
    private double averageEventRating(Event e) {
        Map<UUID, Integer> reviews = e.getReviews();
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    // stream of all events belonging to companies matching the given name
    private java.util.stream.Stream<Event> eventsOfCompanyByName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return java.util.stream.Stream.empty();
        }
        return companyRepository.findActiveByNameContaining(companyName, true).stream()
                .flatMap(company -> company.getAssociatedEventIds().stream())
                .map(id -> eventRepository.findById(id).orElse(null))
                .filter(Objects::nonNull);
    }
    private EventDto toEventDto(Event e) {
        java.time.LocalDateTime start = e.getStartDate() == null ? null
                : e.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDateTime end = e.getEndDate() == null ? null
                : e.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return new EventDto(e.getEventId(), e.getName(), start, end,
                e.getEventType(), e.getVenue(), null);
    }
}
