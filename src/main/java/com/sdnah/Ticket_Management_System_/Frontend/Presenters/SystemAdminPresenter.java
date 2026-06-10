package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Frontend.SystemAdminView;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;


public class SystemAdminPresenter {
    private final SystemAdminView view;
    private final SystemAdminService systemAdminService;
    private final company_managment_serivce companyManagmentService;
    private final NotificationService notificationService;
    private final ActiveOrderService activeOrderService;

    private String selectedTab = "users";
    private String token;

    public SystemAdminPresenter(SystemAdminView view,
            SystemAdminService systemAdminService,
            company_managment_serivce companyManagmentService,
            NotificationService notificationService,
            ActiveOrderService activeOrderService) {
        this.view = view;
        this.systemAdminService = systemAdminService;
        this.companyManagmentService = companyManagmentService;
        this.notificationService = notificationService;
        this.activeOrderService = activeOrderService;
    }

    public boolean hasValidToken(String token) {
        return token != null && !token.isBlank();
    }

    public boolean validateAdminAccess(String token) {
        try {
            systemAdminService.requireAdmin(token);
            this.token = token;
            return true;
        } catch (RuntimeException denied) {
            view.showAccessDenied("You don't have permission to view the admin console.");
            return false;
        }
    }

    public void updateSelectedTab(Map<String, List<String>> params) {
        if (params.containsKey("tab") && !params.get("tab").isEmpty()) {
            selectedTab = params.get("tab").get(0);
        }
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void onLoadUsersClicked() {
        try {
            var users = systemAdminService.getAllUsers(token);
            if (users.isEmpty()) {
                view.displayNoUsersFound();
            } else {
                view.displayUsers(users);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onSuspendUserClicked(String username, String suspensionType, Double hoursValue) {
        if (isEmpty(username)) {
            view.showError("Please enter a Username.");
            return;
        }
        try {
            if ("Permanent".equals(suspensionType)) {
                systemAdminService.suspendPermanently(token, username);
                view.showSuccess("User '" + username + "' suspended permanently.");
            } else {
                long hours = hoursValue == null ? 24 : hoursValue.longValue();
                systemAdminService.suspendUser(token, username, hours);
                view.showSuccess("User '" + username + "' suspended for " + hours + " hours.");
            }
            view.clearSuspendUserForm();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
        
    }

    public void onRemoveMemberClicked(String username) {
        if (isEmpty(username)) {
            view.showError("Please enter a Username.");
            return;
        }

        try {
            systemAdminService.removeMember(token, username);
            view.showSuccess("Member '" + username + "' has been removed.");
            view.clearRemoveMemberForm();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onUnsuspendUserClicked(String username) {
        if (isEmpty(username)) {
            view.showError("Please enter a Username.");
            return;
        }

        try {
            systemAdminService.unsuspendUser(token, username);
            view.showSuccess("User '" + username + "' unsuspended.");
            view.clearUnsuspendUserForm();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onLoadSuspensionsClicked() {
        try {
            var suspensions = systemAdminService.getSuspensions(token);
            if (suspensions.isEmpty()) {
                view.displayNoSuspendedUsersFound();
            } else {
                view.displaySuspensions(suspensions);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onLoadCompaniesClicked() {
        try {
            List<CompanyDTO> companies = companyManagmentService.getActiveCompanies();
            if (companies.isEmpty()) {
                view.displayNoActiveCompaniesFound();
            } else {
                view.displayCompanies(companies);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }

    }

    public void onCloseCompanyClicked(String companyId) {
        if (isEmpty(companyId)) {
            view.showError("Please enter a Company ID.");
            return;
        }

        try {
            companyManagmentService.adminCloseCompany(token, UUID.fromString(companyId));
            view.showSuccess("Company '" + companyId + "' has been closed.");
            view.clearCloseCompanyForm();
        } catch (IllegalArgumentException ex) {
            view.showInvalidCompanyId();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }

    }

    public void onLoadComplaintsClicked() {
        try {
            List<ComplaintDTO> complaints = systemAdminService.getAllSystemComplaintst(token);
            if (complaints.isEmpty()) {
                view.displayNoComplaintsFound();
            } else {
                view.displayComplaints(complaints);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onResolveComplaintClicked(UUID complaintId, String response) {
        if (isEmpty(response)) {
            view.showError("Please enter a response.");
            return;
        }

        try {
            systemAdminService.resolveComplaint(token, complaintId, response);
            view.showSuccess("Complaint resolved.");
            view.markComplaintResolved();
            view.showComplaintResolvedState();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }

    }

    public void onSendSystemMessageClicked(String recipientUsername, String message) {
        if (isEmpty(recipientUsername) || isEmpty(message)) {
            view.showError("Please fill in all fields.");
            return;
        }
        try {
            String memberId = systemAdminService.getMemberIdByUsername(token, recipientUsername);
            notificationService.createNotification(memberId, message, NotificationType.SYSTEM_ANNOUNCEMENT);
            view.showSuccess("Message sent to '" + recipientUsername + "'.");
            view.clearSystemMessageForm();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onAnalyticsTabOpened() {
        try {
            view.displayAnalytics(new AnalyticsData(
                    systemAdminService.getAllUsers(token).size(),
                    systemAdminService.getLoggedInUsersCount(token),
                    systemAdminService.getSuspensions(token).size(),
                    activeOrderService.getActiveOrdersCount(),
                    activeOrderService.getPurchasesTodayCount(),
                    activeOrderService.getReservationRate()));
        } catch (Exception ex) {
            view.showError("Failed to load analytics: " + ex.getMessage());
        }
    }

    public void onLoadQueuesClicked() {
        try {
            List<WaitingQueue> queues = systemAdminService.getAllQueues(token);
            if (queues.isEmpty()) {
                view.displayNoActiveQueuesFound();
            } else {
                view.displayQueues(queues);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onIncreaseQueueFlowClicked(String queueId, Double amountValue) {
        try {
            int amount = amountValue.intValue();
            systemAdminService.increaseQueueFlow(token, Long.parseLong(queueId), amount);
            view.showSuccess("Flow rate increased by " + amount + " for queue '" + queueId + "'.");
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onDecreaseQueueFlowClicked(String queueId, Double amountValue) {
        try {
            int amount = amountValue.intValue();
            systemAdminService.decreaseQueueFlow(token, Long.parseLong(queueId), amount);
            view.showSuccess("Flow rate decreased by " + amount + " for queue '" + queueId + "'.");
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onClearQueueClicked(String queueId) {
        try {
            systemAdminService.clearQueue(token, Long.parseLong(queueId));
            view.showSuccess("Queue '" + queueId + "' cleared.");
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onLoadPurchasesByBuyerClicked(String buyerId) {
        if (isEmpty(buyerId)) {
            view.showError("Please enter a Member ID.");
            return;
        }
        try {
            List<PurchaseDTO> purchases = systemAdminService.getPurchasesByBuyer(token, buyerId);
            if (purchases.isEmpty()) {
                view.displayNoPurchasesByBuyerFound();
            } else {
                view.displayPurchasesByBuyer(purchases);
            }
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    public void onLoadPurchasesByEventClicked(String eventId) {
        if (isEmpty(eventId)) {
            view.showError("Please enter an Event ID.");
            return;
        }
        try {
            List<PurchaseDTO> purchases = systemAdminService.getPurchasesByEvent(token, UUID.fromString(eventId));
            if (purchases.isEmpty()) {
                view.displayNoPurchasesByEventFound();
            } else {
                view.displayPurchasesByEvent(purchases);
            }
        } catch (IllegalArgumentException ex) {
            view.showInvalidEventId();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }
    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static class AnalyticsData {
        private final int totalUsers;
        private final long loggedInNow;
        private final int suspendedUsers;
        private final int activeOrders;
        private final int purchasesToday;
        private final int reservationRate;

        public AnalyticsData(int totalUsers, long loggedInNow, int suspendedUsers,
                int activeOrders, int purchasesToday, int reservationRate) {
            this.totalUsers = totalUsers;
            this.loggedInNow = loggedInNow;
            this.suspendedUsers = suspendedUsers;
            this.activeOrders = activeOrders;
            this.purchasesToday = purchasesToday;
            this.reservationRate = reservationRate;
        }

        public int getTotalUsers() {
            return totalUsers;
        }

        public long getLoggedInNow() {
            return loggedInNow;
        }

        public int getSuspendedUsers() {
            return suspendedUsers;
        }

        public int getActiveOrders() {
            return activeOrders;
        }

        public int getPurchasesToday() {
            return purchasesToday;
        }

        public int getReservationRate() {
            return reservationRate;
        }
    }

}
