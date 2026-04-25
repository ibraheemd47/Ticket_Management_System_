package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class Company {

    private final int companyId;
    private final String companyName;
    private boolean isOpen;
    private final int companyFounderId; 
    private final Set<Integer> ownerIds;

    
    private final List<Integer> associatedEventIds; // II.2.1 
    private final List<Integer> purchaseHistoryIds; // Use Case: II.4.4 4.5
    private final List<Integer> orderHistoryIds;// Use Case: II.4.5 - View company order history
    //private final List<Integer> authorizedManagerIds; // II.4.7 - Manage Company Managers
    private final Map<Integer, Set<CompanyPermission>> managerPermissions;
    private final Map<Integer, Integer> managerAppointedByOwner;

    private double rating;

    public Company(int companyId, String companyName, int companyFounderId) {
        validatePositiveId(companyId, "company id");
        validateNonBlank(companyName, "company name");
        validatePositiveId(companyFounderId, "founder id");

        this.companyId = companyId;
        this.companyName = companyName.trim();
        this.companyFounderId = companyFounderId;
        this.isOpen = true; // II.3.2: opening a new company creates an active company

        this.ownerIds = ConcurrentHashMap.newKeySet();
        this.ownerIds.add(companyFounderId);

        this.associatedEventIds = new CopyOnWriteArrayList<>();
        this.purchaseHistoryIds = new CopyOnWriteArrayList<>();
        this.orderHistoryIds = new CopyOnWriteArrayList<>();

        this.managerPermissions = new ConcurrentHashMap<>();
        this.managerAppointedByOwner = new ConcurrentHashMap<>();
        //this.authorizedManagerIds = new CopyOnWriteArrayList<>();
        this.rating = 0.0;
    }


    //internal validation helpers

    private void validateOwner(int userId) {
        if (!ownerIds.contains(userId)) {
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

    //new method to centralize permission checks for company actions
    public void validateActionPermission(int actorId, CompanyPermission requiredPermission) {
        // Requirement II.4.13: Most actions are blocked if the company is inactive
        if (!isOpen && requiredPermission != CompanyPermission.VIEW_ROLES) {
            throw new IllegalStateException("Operation failed: The company is currently inactive.");
        }
        // 1. Check if actor is an Owner (Full Access)
        if (ownerIds.contains(actorId)) {
            return;
        }
        // 2. Check if actor is a Manager and has the specific permission
        Set<CompanyPermission> permissions = managerPermissions.get(actorId);
        if (permissions != null && permissions.contains(requiredPermission)) {
            return;
        }
        // 3. Fallback: Access Denied
        throw new SecurityException("Unauthorized: Member " + actorId + " lacks permission: " + requiredPermission);
    }



    // --- Use Case: II.2.1 & II.4.1 - View Company Events ---
    public List<Integer> getAssociatedEventIds() {
        if (!isOpen) {
        return new ArrayList<>(); // 
        }
        return new ArrayList<>(associatedEventIds);
    }
    

    // --- Use Case: II.4.1 - Add event to company catalog ---
    public void addEventId(int actorId, int eventId) {
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);
        if (!associatedEventIds.contains(eventId)) {
            associatedEventIds.add(eventId);
        }
        else {
            throw new IllegalArgumentException("Event already exists in company catalog.");
        }
    }

    // --- Use Case: II.4.1 - Remove event from company catalog ---
    public void removeEvent(int eventId) {
        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event does not exist in company catalog");
        }
        associatedEventIds.remove(Integer.valueOf(eventId));
    }

    // --- Use Case: II.4.1 - to Edit existing event ---
    public void validateEventBelongsToCompany(int eventId) {
        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event " + eventId + " does not belong to this company");
        }
    }

        
     //Requirement II.4.2: Define Venue Layout and Event Map.
    public void defineEventLayout(int actorId, int eventId, String mapData) {
        validateActionPermission(actorId, CompanyPermission.MANAGE_EVENTS);
        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("Event not found in this company.");
        }
        // In the Domain, we record that a layout has been defined for this event.
        // The actual map data validation occurs within the Event domain logic.
        //System.out.println("Layout defined for event " + eventId + " by actor " + actorId);
    }

     
    //Requirement II.4.4: Receive and Respond to Inquiries.
    public void respondToInquiry(int actorId, int inquiryId, String response) {
        validateActionPermission(actorId, CompanyPermission.RESPOND_TO_INQUIRIES);
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("Response content cannot be empty.");
        }
        // In Version 1, this logs the management action in the event log.
        System.out.println("Actor " + actorId + " responded to inquiry " + inquiryId);
    }




    // --- Use Case: II.4.5 - Retrieve company history (Purchases & Orders) ---
    public List<Integer> getPurchaseHistoryIds(int actorId) {
        validateActionPermission(actorId, CompanyPermission.VIEW_HISTORY);
        return new ArrayList<>(purchaseHistoryIds);
    }

    public List<Integer> getOrderHistoryIds(int actorId) {
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


    //Requirement II.4.6: Generate Sales Report. 
    public void generateSalesReport(int actorId) {
        // Requires specific permission or ownership 
        validateActionPermission(actorId, CompanyPermission.VIEW_HISTORY);
        // Logic: Gather sales data from direct events and events managed by sub-appointees.
        // For Version 1, this triggers a data collection process across associatedEventIds.
        System.out.println("Generating sales report for actor " + actorId + 
                        " including appointment subtree data.");
    }

    // --- Use Case: II.4.7 - Manage Company Managers ---
    public List<Integer> getAuthorizedManagerIds() { 
        //return Collections.unmodifiableList(authorizedManagerIds);
        return List.copyOf(managerPermissions.keySet());
    }


    //II.4.8 - Appoint Additional Company Owner.
    public synchronized void appointAdditionalOwner(int actingOwnerId, int newOwnerId) {
        validateOwner(actingOwnerId);
        validatePositiveId(newOwnerId, "new owner id");

        if (ownerIds.contains(newOwnerId)) {
            throw new IllegalArgumentException("User is already an owner of this company.");
        }

        if (managerPermissions.containsKey(newOwnerId)) {
            throw new IllegalArgumentException("A manager cannot be promoted to owner without removing the manager role first.");
        }

        ownerIds.add(newOwnerId);
    }

    //II.4.9 - Remove Company Owner Appointment.
    public synchronized void removeOwnerAppointment(int actingOwnerId, int targetOwnerId) {
        validateOwner(actingOwnerId);

        if (!ownerIds.contains(targetOwnerId)) {
            throw new IllegalArgumentException("Target user is not an owner of this company.");
        }

        if (isFounder(targetOwnerId)) {
            throw new IllegalArgumentException("The founder cannot be removed from ownership.");
        }

        if (ownerIds.size() == 1) {
            throw new IllegalStateException("An active company must have at least one owner.");
        }

        ownerIds.remove(targetOwnerId);
    }

    //II.4.10 - Resign from Ownership.
    public synchronized void resignOwnership(int ownerId) {
        if (!ownerIds.contains(ownerId)) {
            throw new SecurityException("User is not an owner of this company.");
        }

        if (isFounder(ownerId)) {
            throw new IllegalArgumentException("Founder cannot resign from ownership.");
        }

        if (ownerIds.size() == 1) {
            throw new IllegalStateException("An active company must have at least one owner.");
        }

        ownerIds.remove(ownerId);
    }

    //II.4.7 - Appoint Company Manager.
    public synchronized void appointManager(int appointingOwnerId,
                                            int managerId,
                                            Set<CompanyPermission> permissions) {
        validateOwner(appointingOwnerId);
        validatePositiveId(managerId, "manager id");

        if (ownerIds.contains(managerId)) {
            throw new IllegalArgumentException("An owner cannot be appointed as a manager.");
        }

        if (managerPermissions.containsKey(managerId)) {
            throw new IllegalArgumentException("Manager already exists.");
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions set cannot be null.");
        }

        managerPermissions.put(managerId, EnumSet.copyOf(permissions.isEmpty()
                ? EnumSet.noneOf(CompanyPermission.class)
                : permissions));
        managerAppointedByOwner.put(managerId, appointingOwnerId);
    }

    //II.4.11 - Modify Manager Permissions.
    public synchronized void modifyManagerPermissions(int actingOwnerId,
                                                      int managerId,
                                                      Set<CompanyPermission> updatedPermissions) {
        validateOwner(actingOwnerId);

        if (!managerPermissions.containsKey(managerId)) {
            throw new IllegalArgumentException("Manager does not exist.");
        }

        Integer appointingOwner = managerAppointedByOwner.get(managerId);
        if (appointingOwner == null || appointingOwner != actingOwnerId) {
            throw new SecurityException("Only the appointing owner can modify this manager's permissions.");
        }

        if (updatedPermissions == null) {
            throw new IllegalArgumentException("Updated permissions set cannot be null.");
        }

        managerPermissions.put(managerId, EnumSet.copyOf(updatedPermissions.isEmpty()
                ? EnumSet.noneOf(CompanyPermission.class)
                : updatedPermissions));
    }

    //II.4.12 - Remove Manager Appointment.
    public synchronized void removeManagerAppointment(int actingOwnerId, int managerId) {
        validateOwner(actingOwnerId);

        if (!managerPermissions.containsKey(managerId)) {
            throw new IllegalArgumentException("Manager does not exist.");
        }

        Integer appointingOwner = managerAppointedByOwner.get(managerId);
        if (appointingOwner == null || appointingOwner != actingOwnerId) {
            throw new SecurityException("Only the appointing owner can remove this manager appointment.");
        }

        managerPermissions.remove(managerId);
        managerAppointedByOwner.remove(managerId);
    }

    //II.5 - Perform Management Action (Company Manager).
    public boolean managerHasPermission(int managerId, CompanyPermission permission) {
        Set<CompanyPermission> permissions = managerPermissions.get(managerId);
        return permissions != null && permissions.contains(permission);
    }

    //II.4.13 - Suspend / Close Production Company.
    public synchronized boolean closeCompany(int actingFounderId) {
        if (!isFounder(actingFounderId)) {
            throw new SecurityException("Only the founder can close the company.");
        }

        if (!isOpen) {
            return false;
        }

        this.isOpen = false;
        return true;
    }

    //II.4.14 - Reopen Production Company.
    public synchronized boolean reopenCompany(int actingFounderId) {
        if (!isFounder(actingFounderId)) {
            throw new SecurityException("Only the founder can reopen the company.");
        }

        if (isOpen) {
            return false;
        }

        this.isOpen = true;
        return true;
    }

    


    
    // Getters

    public int getCompanyId() { return companyId; }

    public String getCompanyName() { return companyName; }

    public boolean isOpen() { return isOpen; }

    public int getCompanyFounderId() { return companyFounderId; }

    public List<Integer> getManagers() {
        
        return List.copyOf(managerPermissions.keySet());
    }

    public void setOpen(boolean isOpen2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setOpen'");
    }

    public boolean isFounder(int userId) {
        return companyFounderId == userId;
    }

    public boolean isOwner(int userId) {
        return ownerIds.contains(userId);
    }

    public boolean isManager(int userId) {
        return managerPermissions.containsKey(userId);
    }

    public List<Integer> getOwnerIds() {
        return List.copyOf(ownerIds);
    }


    public Map<Integer, Set<CompanyPermission>> getManagerPermissionsView() {
        Map<Integer, Set<CompanyPermission>> copy = new HashMap<>();
        for (Map.Entry<Integer, Set<CompanyPermission>> entry : managerPermissions.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    //helper
    public boolean matchesName(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return true; // אין סינון
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

    public void hasEvent(int eventId) {
        if (!isOpen) {
            throw new IllegalStateException("Company is inactive.");
        }
        if (!associatedEventIds.contains(eventId)) {
            throw new NoSuchElementException("Event does not belong to this company.");
        }
    }
}
