package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class Company {

    private final int companyId;
    private final String companyName;
    private boolean isOpen;
    private final int companyFounderId; // מזהה נומרי למייסד

    // POLICY
    // private PurchasePolicy purchasePolicy;
    // private DiscountPolicy discountPolicy;
    // private SellingPolicy sellingPolicy;

    // ניהול מזהים בלבד לשמירה על הפרדת שכבות 
    private final List<Integer> associatedEventIds; // II.2.1 
    private final List<Integer> purchaseHistoryIds; // Use Case: II.4.4 4.5
    private final List<Integer> orderHistoryIds;// Use Case: II.4.5 - View company order history
    private final List<Integer> authorizedManagerIds;// Use Case: II.4.7 - Appoint Company Manager

    public Company(int companyId, String companyName, int companyFounderId ) {
        // אילוצי נכונות (Validation) - גרסה 1 דורשת אכיפה במבנה הפעולות [cite: 1098]
        if (companyId <= 0) {
            throw new IllegalArgumentException("company id must be positive");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("company name cannot be empty");
        }
        if (companyFounderId <= 0) {
            throw new IllegalArgumentException("founder ID must be positive");
        }
        // if (purchasePolicy == null || discountPolicy == null || sellingPolicy == null) {
        //     throw new IllegalArgumentException("policies cannot be null");
        // }

        this.companyId = companyId;
        this.companyName = companyName;
        this.isOpen = true; // (Use Case II.1.1) 
        this.companyFounderId = companyFounderId;
        // this.purchasePolicy = purchasePolicy;
        // this.discountPolicy = discountPolicy;
        // this.sellingPolicy = sellingPolicy;

        this.associatedEventIds = new CopyOnWriteArrayList<>();
        this.purchaseHistoryIds = new CopyOnWriteArrayList<>();
        this.orderHistoryIds = new CopyOnWriteArrayList<>();
        this.authorizedManagerIds = new CopyOnWriteArrayList<>();
    }

    // --- Use Case: II.2.1 & II.4.1 - View Company Events ---

    public List<Integer> getAssociatedEventIds() {
        return Collections.unmodifiableList(associatedEventIds);
    }

    // --- Use Case: II.4.1 - Add event to company catalog ---
    public void addEvent(int eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("event ID must be positive");
        }
        // אילוץ נכונות: מניעת כפילויות כפי שנדרש בגרסה 1
        if (associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event already exists in company catalog");
        }
        associatedEventIds.add(eventId);
    }

    // --- Use Case: II.4.1 - Remove event from company catalog ---
    public void removeEvent(int eventId) {
        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event does not exist in company catalog");
        }
        associatedEventIds.remove(Integer.valueOf(eventId));
    }

    // --- Use Case: II.4.1 - Edit existing event ---
    /**
     * ב-Domain מבוסס IDs, עדכון אירוע לרוב מתבצע ישירות על אובייקט ה-Event.
     * כאן אנחנו רק מוודאים שהאירוע אכן שייך לחברה.
     */
    public void validateEventBelongsToCompany(int eventId) {
        if (!associatedEventIds.contains(eventId)) {
            throw new IllegalArgumentException("event " + eventId + " does not belong to this company");
        }
    }

    // --- Use Case: II.4.5 - Retrieve company history (Purchases & Orders) ---
    public List<Integer> getPurchaseHistoryIds() {
        return Collections.unmodifiableList(purchaseHistoryIds);
    }

    public List<Integer> getOrderHistoryIds() {
        return Collections.unmodifiableList(orderHistoryIds);
    }

    public void addPurchaseRecord(int purchaseId) {
        purchaseHistoryIds.add(purchaseId);
    }

    // --- Use Case: II.4.7 - Manage Company Managers ---
    public List<Integer> getAuthorizedManagerIds() {
        return Collections.unmodifiableList(authorizedManagerIds);
    }

    public void appointManager(int managerId) {
        if (managerId <= 0) {
            throw new IllegalArgumentException("manager ID must be positive");
        }
// Integrity Constraint: The founder/owner cannot be appointed as a manager (based on defined business logic).
        if (managerId == this.companyFounderId) {
            throw new IllegalArgumentException("Founder is already an owner and cannot be appointed as manager");
        }
        if (authorizedManagerIds.contains(managerId)) {
            throw new IllegalArgumentException("manager already exists");
        }
        authorizedManagerIds.add(managerId);
    }

    

    // Getters
    public int getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public boolean isOpen() { return isOpen; }
    public int getCompanyFounderId() { return companyFounderId; }

    public List<Integer> getManagers() {
        // TODO Auto-generated method stub
        //throw new UnsupportedOperationException("Unimplemented method 'getManagers'");
        return Collections.unmodifiableList(authorizedManagerIds);
    }

    public void setOpen(boolean isOpen2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setOpen'");
    }

    
}
