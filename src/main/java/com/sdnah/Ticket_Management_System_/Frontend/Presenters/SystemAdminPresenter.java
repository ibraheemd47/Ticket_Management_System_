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
          //  view.showAccessDenied("You don't have permission to view the admin console.");
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
        
    }

    public void onSuspendUserClicked(String username, String suspensionType, Double hoursValue) {
        
    }

    public void onRemoveMemberClicked(String username) {
       
    }

    public void onUnsuspendUserClicked(String username) {
        
    }

    public void onLoadSuspensionsClicked() {
       
    }

    public void onLoadCompaniesClicked() {
        
    }

    public void onCloseCompanyClicked(String companyId) {
        
    }

    public void onLoadComplaintsClicked() {
        
    }

    public void onResolveComplaintClicked(UUID complaintId, String response) {
        
    }

    public void onSendSystemMessageClicked(String recipientUsername, String message) {
        
    }

    public void onAnalyticsTabOpened() {
        
    }

    public void onLoadQueuesClicked() {
        
    }

    public void onIncreaseQueueFlowClicked(String queueId, Double amountValue) {
        
    }

    public void onDecreaseQueueFlowClicked(String queueId, Double amountValue) {
        
    }

    public void onClearQueueClicked(String queueId) {
        
    }

    public void onLoadPurchasesByBuyerClicked(String buyerId) {
        
    }

    public void onLoadPurchasesByEventClicked(String eventId) {
        
    }

}
