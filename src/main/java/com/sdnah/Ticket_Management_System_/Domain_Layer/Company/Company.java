package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Company {

    private final int companyId;
    private final String companyName;
    private boolean isOpen;

    private final String companyFounderId;
    private final Set<String> ownerIds;

    private final List<Integer> associatedEventIds;
    private final List<Integer> purchaseHistoryIds;
    private final List<Integer> orderHistoryIds;

    private final Map<String, Set<CompanyPermission>> managerPermissions;
    private final Map<String, String> managerAppointedByOwner;
    private final Map<String, String> ownerAppointedByOwner;

    private double rating;
    private String logoURL;

    public Company(int companyId, String companyName, String companyFounderId) {
        validatePositiveId(companyId, "company id");
        validateNonBlank(companyName, "company name");
        validateNonBlank(companyFounderId, "founder id");

        this.companyId = companyId;
        this.companyName = companyName.trim();
        this.companyFounderId = companyFounderId.trim();
        this.isOpen = true;

        this.ownerIds = ConcurrentHashMap.newKeySet();
        this.ownerIds.add(this.companyFounderId);

        this.associatedEventIds = new CopyOnWriteArrayList<>();
        this.purchaseHistoryIds = new CopyOnWriteArrayList<>();
        this.orderHistoryIds = new CopyOnWriteArrayList<>();

        this.managerPermissions = new ConcurrentHashMap<>();
        this.managerAppointedByOwner = new ConcurrentHashMap<>();
        this.ownerAppointedByOwner = new ConcurrentHashMap<>();

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

        Set<CompanyPermission> permissions = managerPermissions.get(normalizedActorId);
        if (permissions != null && permissions.contains(requiredPermission)) {
            return;
        }

        throw new SecurityException("Unauthorized: Member " + normalizedActorId
                + " lacks permission: " + requiredPermission);
    }

    public List<Integer> getAssociatedEventIds() {
        if (!isOpen) {
            return new ArrayList<>();
        }
        return new ArrayList<>(associatedEventIds);
    }

    public synchronized void addEventId(String actorId, int eventId) {
        validatePositiveId(eventId, "event id");
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);

        if (associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("Event already exists in company catalog.");
        }

        associatedEventIds.add(eventId);
    }

    public synchronized void removeEvent(String actorId, int eventId) {
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);

        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event does not exist");
        }

        associatedEventIds.remove(Integer.valueOf(eventId));
    }

    public void validateEventBelongsToCompany(int eventId) {
        validatePositiveId(eventId, "event id");

        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event " + eventId + " does not belong to this company");
        }
    }

    public void defineEventLayout(String actorId, int eventId, String mapData) {
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);
        validatePositiveId(eventId, "event id");

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
        return List.copyOf(managerPermissions.keySet());
    }

    public synchronized void appointAdditionalOwner(String actingOwnerId, String newOwnerId) {
        validateOwner(actingOwnerId);
        validateNonBlank(newOwnerId, "new owner id");

        String normalizedNewOwnerId = newOwnerId.trim();

        if (ownerIds.contains(normalizedNewOwnerId)) {
            throw new IllegalArgumentException("User is already an owner of this company.");
        }

        if (managerPermissions.containsKey(normalizedNewOwnerId)) {
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

        if (managerPermissions.containsKey(normalizedManagerId)) {
            throw new IllegalArgumentException("Manager already exists.");
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions set cannot be null.");
        }

        managerPermissions.put(
                normalizedManagerId,
                EnumSet.copyOf(permissions.isEmpty()
                        ? EnumSet.noneOf(CompanyPermission.class)
                        : permissions)
        );

        managerAppointedByOwner.put(normalizedManagerId, appointingOwnerId.trim());
    }

    public synchronized void modifyManagerPermissions(String actingOwnerId,
                                                      String managerId,
                                                      Set<CompanyPermission> updatedPermissions) {
        validateOwner(actingOwnerId);
        validateNonBlank(managerId, "manager id");

        String normalizedActingOwnerId = actingOwnerId.trim();
        String normalizedManagerId = managerId.trim();

        if (!managerPermissions.containsKey(normalizedManagerId)) {
            throw new IllegalArgumentException("Manager does not exist.");
        }

        String appointingOwner = managerAppointedByOwner.get(normalizedManagerId);
        if (appointingOwner == null || !appointingOwner.equals(normalizedActingOwnerId)) {
            throw new SecurityException("Only the appointing owner can modify this manager's permissions.");
        }

        if (updatedPermissions == null) {
            throw new IllegalArgumentException("Updated permissions set cannot be null.");
        }

        managerPermissions.put(
                normalizedManagerId,
                EnumSet.copyOf(updatedPermissions.isEmpty()
                        ? EnumSet.noneOf(CompanyPermission.class)
                        : updatedPermissions)
        );
    }

    public synchronized void removeManagerAppointment(String actingOwnerId, String managerId) {
        validateOwner(actingOwnerId);
        validateNonBlank(managerId, "manager id");

        String normalizedActingOwnerId = actingOwnerId.trim();
        String normalizedManagerId = managerId.trim();

        if (!managerPermissions.containsKey(normalizedManagerId)) {
            throw new IllegalArgumentException("Manager does not exist.");
        }

        String appointingOwner = managerAppointedByOwner.get(normalizedManagerId);
        if (appointingOwner == null || !appointingOwner.equals(normalizedActingOwnerId)) {
            throw new SecurityException("Only the appointing owner can remove this manager appointment.");
        }

        managerPermissions.remove(normalizedManagerId);
        managerAppointedByOwner.remove(normalizedManagerId);
    }

    public boolean managerHasPermission(String managerId, CompanyPermission permission) {
        validateNonBlank(managerId, "manager id");

        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null.");
        }

        Set<CompanyPermission> permissions = managerPermissions.get(managerId.trim());
        return permissions != null && permissions.contains(permission);
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

    public int getCompanyId() {
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
        return List.copyOf(managerPermissions.keySet());
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
        return managerPermissions.containsKey(userId.trim());
    }

    public List<String> getOwnerIds() {
        return List.copyOf(ownerIds);
    }

    public Map<String, Set<CompanyPermission>> getManagerPermissionsView() {
        Map<String, Set<CompanyPermission>> copy = new HashMap<>();

        for (Map.Entry<String, Set<CompanyPermission>> entry : managerPermissions.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
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

    public boolean hasEvent(int eventId) {
        validatePositiveId(eventId, "event id");

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
                "Managers: " + managerPermissions.keySet() + "\n" +
                "Associated Events: " + associatedEventIds;
    }
}