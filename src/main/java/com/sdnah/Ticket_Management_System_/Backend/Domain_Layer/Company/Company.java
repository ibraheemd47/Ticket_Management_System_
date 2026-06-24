package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company;

import java.util.*;
import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false, unique = true)
    private String companyName;

    @Column(name = "is_open")
    private boolean isOpen;

    @Column(nullable = false)
    private String companyFounderId;

    // EAGER: needed on every auth check (is caller an owner?)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_owners", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "owner_id")
    private Set<String> ownerIds = new HashSet<>();

    // EAGER: needed when listing a company's events on every page load
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_events", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "event_id", columnDefinition = "uuid")
    private List<UUID> associatedEventIds = new ArrayList<>();

    // LAZY: only accessed in billing/admin screens, not on every company load
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_purchase_history", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "purchase_id")
    private List<Integer> purchaseHistoryIds = new ArrayList<>();

    // LAZY: only accessed in billing/admin screens, not on every company load
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_order_history", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "order_id")
    private List<Integer> orderHistoryIds = new ArrayList<>();

    // Per-user reviews (1–5). The aggregate `rating` below is recomputed as the
    // average of these whenever a member adds/updates their review, mirroring how
    // Event stores reviews.
    @ElementCollection
    @CollectionTable(name = "company_reviews", joinColumns = @JoinColumn(name = "company_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "rating")
    private Map<UUID, Integer> usersReviews = new HashMap<>();

    private double rating;
    private String logoURL;

    // EAGER: needed on every auth check (is caller a manager?)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_managers", joinColumns = @JoinColumn(name = "company_id"))
    private Set<CompanyManagerAssignment> managers = new HashSet<>();

    // LAZY: only accessed when auditing who appointed whom (rare admin operation)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_owner_appointments", joinColumns = @JoinColumn(name = "company_id"))
    @MapKeyColumn(name = "owner_id")
    @Column(name = "appointed_by_owner_id")
    private Map<String, String> ownerAppointedByOwner = new HashMap<>();

    protected Company() {
    // required by JPA
    }

    public Company(String companyName, String companyFounderId) {
        validateNonBlank(companyName, "company name");
        validateNonBlank(companyFounderId, "founder id");

        this.companyId = UUID.randomUUID();
        this.companyName = companyName.trim();
        this.companyFounderId = companyFounderId.trim();
        this.isOpen = true;
        this.ownerIds.add(this.companyFounderId);
        this.rating = 0.0;
    }

    private void validateOwner(String userId) {
        validateNonBlank(userId, "user id");

        if (!ownerIds.contains(userId.trim())) {
            throw new SecurityException("User is not an owner of this company.");
        }
    }

    private void validatePositiveId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
    }

    private void validateNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }

    public void validateActionPermission(String actorId, CompanyPermission requiredPermission) {
        validateNonBlank(actorId, "actor id");

        if (requiredPermission == null) {
            throw new IllegalArgumentException("Required permission cannot be null.");
        }

        String normalizedActorId = actorId.trim();

        if (!isOpen && requiredPermission != CompanyPermission.VIEW_ROLES) {
            throw new IllegalStateException("Operation failed: The company is currently inactive.");
        }

        if (ownerIds.contains(normalizedActorId)) {
            return;
        }

        Optional<CompanyManagerAssignment> assignment = findManagerAssignment(normalizedActorId);
        if (assignment.isPresent() && assignment.get().hasPermission(requiredPermission)) {
            return;
        }

        throw new SecurityException("Unauthorized: Member " + normalizedActorId
                + " lacks permission: " + requiredPermission);
    }

    public List<UUID> getAssociatedEventIds() {
        if (!isOpen) {
            return new ArrayList<>();
        }
        return new ArrayList<>(associatedEventIds);
    }

    public synchronized void addEventId(String actorId, UUID eventId) {
        if (eventId == null) {
        throw new IllegalArgumentException("event id cannot be null.");
    }
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);

        if (associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("Event already exists in company catalog.");
        }

        associatedEventIds.add(eventId);
    }

    public synchronized void removeEvent(String actorId, UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("event id cannot be null.");
        }

        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);

        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event does not exist");
        }

        associatedEventIds.remove(eventId);
    }

    /**
     * Drop {@code eventId} from {@link #associatedEventIds} without permission
     * or existence checks. Cleanup helper for when an event has already been
     * deleted at the Event aggregate level and we just need to forget its id
     * here. No-op if the id isn't present.
     */
    public synchronized void removeEventIfPresent(UUID eventId) {
        if (eventId != null) {
            associatedEventIds.remove(eventId);
        }
    }

    public void validateEventBelongsToCompany(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("event id cannot be null.");
        }

        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event " + eventId + " does not belong to this company");
        }
    }

    public void defineEventLayout(String actorId, UUID eventId, String mapData) {
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);
        if (eventId == null) {
            throw new IllegalArgumentException("event id cannot be null.");
        }

        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("Event not found in this company.");
        }
    }

    public void respondToInquiry(String actorId, int inquiryId, String response) {
        validateActionPermission(actorId, CompanyPermission.RESPOND_TO_INQUIRIES);
        validatePositiveId(inquiryId, "inquiry id");

        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("Response content cannot be empty.");
        }

        System.out.println("Actor " + actorId + " responded to inquiry " + inquiryId);
    }

    public List<Integer> getPurchaseHistoryIds(String actorId) {
        validateActionPermission(actorId, CompanyPermission.VIEW_HISTORY);
        return new ArrayList<>(purchaseHistoryIds);
    }

    public List<Integer> getOrderHistoryIds(String actorId) {
        validateActionPermission(actorId, CompanyPermission.VIEW_HISTORY);
        return new ArrayList<>(orderHistoryIds);
    }

    public synchronized void addPurchaseRecord(int purchaseId) {
        validatePositiveId(purchaseId, "purchase id");
        purchaseHistoryIds.add(purchaseId);
    }

    public synchronized void addOrderRecord(int orderId) {
        validatePositiveId(orderId, "order id");
        orderHistoryIds.add(orderId);
    }

    public void generateSalesReport(String actorId) {
        validateActionPermission(actorId, CompanyPermission.VIEW_HISTORY);

        System.out.println("Generating sales report for actor " + actorId
                + " including appointment subtree data.");
    }

    public List<String> getAuthorizedManagerIds() {
        return managers.stream()
                .map(CompanyManagerAssignment::getManagerId)
                .toList();
    }

    public synchronized void appointAdditionalOwner(String actingOwnerId, String newOwnerId) {
        validateOwner(actingOwnerId);
        validateNonBlank(newOwnerId, "new owner id");

        String normalizedNewOwnerId = newOwnerId.trim();

        if (ownerIds.contains(normalizedNewOwnerId)) {
            throw new IllegalArgumentException("User is already an owner of this company.");
        }

        if (findManagerAssignment(normalizedNewOwnerId).isPresent()) {
            throw new IllegalArgumentException(
                    "A manager cannot be promoted to owner without removing the manager role first.");
        }

        ownerIds.add(normalizedNewOwnerId);
        ownerAppointedByOwner.put(normalizedNewOwnerId, actingOwnerId.trim());
    }

    public synchronized void removeOwnerAppointment(String actingOwnerId, String targetOwnerId) {
        validateOwner(actingOwnerId);
        validateNonBlank(targetOwnerId, "target owner id");

        String normalizedActingOwnerId = actingOwnerId.trim();
        String normalizedTargetOwnerId = targetOwnerId.trim();

        if (!ownerIds.contains(normalizedTargetOwnerId)) {
            throw new IllegalArgumentException("Target user is not an owner of this company.");
        }

        if (isFounder(normalizedTargetOwnerId)) {
            throw new IllegalArgumentException("The founder cannot be removed from ownership.");
        }

        String appointingOwner = ownerAppointedByOwner.get(normalizedTargetOwnerId);
        if (appointingOwner == null || !appointingOwner.equals(normalizedActingOwnerId)) {
            throw new SecurityException("Only the owner who appointed this owner can remove the appointment.");
        }

        if (ownerIds.size() == 1) {
            throw new IllegalStateException("An active company must have at least one owner.");
        }

        ownerIds.remove(normalizedTargetOwnerId);
        ownerAppointedByOwner.remove(normalizedTargetOwnerId);
    }

    public synchronized void resignOwnership(String ownerId) {
        validateNonBlank(ownerId, "owner id");

        String normalizedOwnerId = ownerId.trim();

        if (!ownerIds.contains(normalizedOwnerId)) {
            throw new SecurityException("User is not an owner of this company.");
        }

        if (isFounder(normalizedOwnerId)) {
            throw new IllegalArgumentException("Founder cannot resign from ownership.");
        }

        if (ownerIds.size() == 1) {
            throw new IllegalStateException("An active company must have at least one owner.");
        }

        ownerIds.remove(normalizedOwnerId);
        ownerAppointedByOwner.remove(normalizedOwnerId);
    }

    public synchronized void appointManager(String appointingOwnerId,
                                        String managerId,
                                        Set<CompanyPermission> permissions) {
        validateOwner(appointingOwnerId);
        validateNonBlank(managerId, "manager id");

        String normalizedManagerId = managerId.trim();

        if (ownerIds.contains(normalizedManagerId)) {
            throw new IllegalArgumentException("An owner cannot be appointed as a manager.");
        }

        if (findManagerAssignment(normalizedManagerId).isPresent()) {
            throw new IllegalArgumentException("Manager already exists.");
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions set cannot be null.");
        }

        managers.add(new CompanyManagerAssignment(
                normalizedManagerId,
                appointingOwnerId.trim(),
                permissions
        ));
    }

    public synchronized void modifyManagerPermissions(String actingOwnerId,
                                                  String managerId,
                                                  Set<CompanyPermission> updatedPermissions) {
        validateOwner(actingOwnerId);
        validateNonBlank(managerId, "manager id");

        String normalizedActingOwnerId = actingOwnerId.trim();

        CompanyManagerAssignment assignment = findManagerAssignment(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager does not exist."));

        if (!assignment.getAppointedByOwnerId().equals(normalizedActingOwnerId)) {
            throw new SecurityException("Only the appointing owner can modify this manager's permissions.");
        }

        if (updatedPermissions == null) {
            throw new IllegalArgumentException("Updated permissions set cannot be null.");
        }

        assignment.setPermissions(updatedPermissions);
    }

    public synchronized void removeManagerAppointment(String actingOwnerId, String managerId) {
        validateOwner(actingOwnerId);
        validateNonBlank(managerId, "manager id");

        String normalizedActingOwnerId = actingOwnerId.trim();

        CompanyManagerAssignment assignment = findManagerAssignment(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager does not exist."));

        if (!assignment.getAppointedByOwnerId().equals(normalizedActingOwnerId)) {
            throw new SecurityException("Only the appointing owner can remove this manager appointment.");
        }

        managers.remove(assignment);
    }

    public boolean managerHasPermission(String managerId, CompanyPermission permission) {
        validateNonBlank(managerId, "manager id");

        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null.");
        }

        return findManagerAssignment(managerId)
                .map(m -> m.hasPermission(permission))
                .orElse(false);
    }

    public synchronized boolean reopenCompany(String actingFounderId) {
        validateNonBlank(actingFounderId, "founder id");

        if (!isFounder(actingFounderId)) {
            throw new SecurityException("Only the founder can reopen the company.");
        }

        if (isOpen) {
            return false;
        }

        this.isOpen = true;
        return true;
    }

    public synchronized boolean closeCompany(String actingFounderId) {
        validateNonBlank(actingFounderId, "founder id");

        if (!isFounder(actingFounderId)) {
            throw new SecurityException("Only the founder can close the company.");
        }

        if (!isOpen) {
            return false;
        }

        this.isOpen = false;
        return true;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public String getCompanyFounderId() {
        return companyFounderId;
    }

    public List<String> getManagers() {
        return managers.stream()
                .map(CompanyManagerAssignment::getManagerId)
                .toList();
    }

    public boolean isFounder(String userId) {
        validateNonBlank(userId, "user id");
        return companyFounderId.equals(userId.trim());
    }

    public boolean isOwner(String userId) {
        validateNonBlank(userId, "user id");
        return ownerIds.contains(userId.trim());
    }

    public boolean isManager(String userId) {
        validateNonBlank(userId, "user id");
        return findManagerAssignment(userId).isPresent();
    }

    public List<String> getOwnerIds() {
        return List.copyOf(ownerIds);
    }

    public Map<String, Set<CompanyPermission>> getManagerPermissionsView() {
        Map<String, Set<CompanyPermission>> copy = new HashMap<>();

        for (CompanyManagerAssignment manager : managers) {
            copy.put(manager.getManagerId(), Set.copyOf(manager.getPermissions()));
        }

        return Collections.unmodifiableMap(copy);
    }

    /**
     * Read-only map: managerId → memberId of the owner who appointed them.
     * Used by the "Roles" tab so the UI can show provenance next to each
     * manager (II.4.7 / II.4.15 — view roles and appointments).
     */
    public Map<String, String> getManagerAppointedByView() {
        Map<String, String> copy = new HashMap<>();
        for (CompanyManagerAssignment manager : managers) {
            copy.put(manager.getManagerId(), manager.getAppointedByOwnerId());
        }
        return Collections.unmodifiableMap(copy);
    }

    public boolean matchesName(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return true;
        }

        return this.companyName.toLowerCase()
                .contains(companyName.toLowerCase().trim());
    }

    public double getRating() {
        return rating;
    }

    public void updateRating(double rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        }

        this.rating = rating;
    }

    /**
     * Records (or replaces) a member's 1–5 review of this company and refreshes
     * the aggregate {@link #rating} to the average of all reviews.
     */
    public void addReview(UUID userId, int rating) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        usersReviews.put(userId, rating);
        recomputeRating();
    }

    private void recomputeRating() {
        this.rating = usersReviews.isEmpty()
                ? 0.0
                : usersReviews.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public Map<UUID, Integer> getReviews() {
        return usersReviews;
    }

    public boolean hasEvent(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("event id cannot be null.");
        }

        if (!isOpen) {
            throw new IllegalStateException("Company is inactive.");
        }

        if (!associatedEventIds.contains(eventId)) {
            throw new NoSuchElementException("Event does not belong to this company.");
        }

        return true;
    }

    public String getLogoURL() {
        return logoURL;
    }

    public void setLogoURL(String logoURL) {
        validateNonBlank(logoURL, "Logo URL");
        this.logoURL = logoURL.trim();
    }

    public String getFullDetails() {
        return "Company Name: " + companyName + "\n" +
                "Company ID: " + companyId + "\n" +
                "Founder ID: " + companyFounderId + "\n" +
                "Status: " + (isOpen ? "Active" : "Inactive") + "\n" +
                "Rating: " + rating + "\n" +
                "Logo URL: " + (logoURL != null ? logoURL : "No logo set") + "\n" +
                "Owners: " + ownerIds + "\n" +
                "Managers: " + getManagers() + "\n" +
                "Associated Events: " + associatedEventIds;
    }


    public synchronized boolean adminCloseCompany() {
        if (!isOpen) {
            return false;
        }

        this.isOpen = false;

        ownerIds.clear();
        ownerAppointedByOwner.clear();
        managers.clear();

        return true;
    }

    //helper function
    private Optional<CompanyManagerAssignment> findManagerAssignment(String managerId) {
        validateNonBlank(managerId, "manager id");

        String normalizedManagerId = managerId.trim();

        return managers.stream()
                .filter(m -> m.getManagerId().equals(normalizedManagerId))
                .findFirst();
    }
}