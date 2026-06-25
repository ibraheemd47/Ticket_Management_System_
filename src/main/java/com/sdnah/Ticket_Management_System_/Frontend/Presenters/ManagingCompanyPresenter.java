package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.SalesReportDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Frontend.ManagingCompanyView;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link ManagingCompanyView}. Wraps the three service
 * collaborators (company / user / policy), owns the resolved
 * (token, companyId) for the session, and exposes one method per action
 * the view triggers.
 */
@Component
@UIScope
public class ManagingCompanyPresenter {

    private final company_managment_serivce companyService;
    private final UserService userService;
    private final PolicyService policyService;
    private final ComplaintService complaintService;

    private ManagingCompanyView view;
    private String token;
    private UUID companyId;

    public ManagingCompanyPresenter(company_managment_serivce companyService,
                                    UserService userService,
                                    PolicyService policyService,
                                    ComplaintService complaintService) {
        this.companyService = companyService;
        this.userService = userService;
        this.policyService = policyService;
        this.complaintService = complaintService;
    }

    public void setView(ManagingCompanyView view) {
        this.view = view;
    }

    /** Snapshot session-derived state for subsequent service calls. */
    public void bind(String token, UUID companyId) {
        this.token = token;
        this.companyId = companyId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    /**
     * Defensive guard against stale session state — verifies the currently
     * bound user has *any* role assignment in the bound company. Used by the
     * view before it renders the detail shell, so a {@code managingCompanyId}
     * left over in the session by a previous user can't leak that company's
     * details to whoever logs in next.
     *
     * @return {@code true} iff the resolved member has a role on the bound
     *         company; {@code false} on missing state or any failure
     *         (lookup error → treat as denied).
     */
    public boolean userHasAccessToCurrentCompany() {
        if (token == null || companyId == null) return false;
        try {
            Member me = userService.getMemberByToken(token);
            for (CompanyRoleAssignment a : me.getCompanyRoles()) {
                if (companyId.equals(a.getCompanyId())) return true;
            }
            return false;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // ── Chooser (list mode) ─────────────────────────────────────────────────

    /** Load the companies this member owns/manages and pass them to the view. */
    public void loadMyCompanies() {
        try {
            Member me = userService.getMemberByToken(token);
            view.showMyCompanies(resolveMyCompanies(me));
        } catch (RuntimeException ex) {
            view.showCompanyChooserError("Couldn't load your companies: " + ex.getMessage());
        }
    }

    /** Cross-reference the member's roles with the active-companies list to get names. */
    private List<CompanyRow> resolveMyCompanies(Member me) {
        Set<UUID> myCompanyIds = new HashSet<>();
        Map<UUID, String> roleByCompany = new HashMap<>();
        for (CompanyRoleAssignment a : me.getCompanyRoles()) {
            myCompanyIds.add(a.getCompanyId());
            // First role wins if there are duplicates.
            roleByCompany.putIfAbsent(a.getCompanyId(),
                    a.isOwner() ? "Owner" : a.isManager() ? "Manager" : a.getRoleType().name());
        }

        // Best available source for company names today; switch to a dedicated
        // "find by ids" query if/when one is added.
        Map<UUID, String> nameById = new HashMap<>();
        try {
            for (CompanyDTO dto : companyService.getActiveCompanies()) {
                nameById.put(dto.getCompanyId(), dto.getCompanyName());
            }
        } catch (RuntimeException ignored) {
            // If the lookup blows up we still show IDs.
        }

        List<CompanyRow> out = new ArrayList<>();
        for (UUID cid : myCompanyIds) {
            out.add(new CompanyRow(cid, nameById.get(cid), roleByCompany.get(cid)));
        }
        out.sort((a, b) -> a.companyId.compareTo(b.companyId));
        return out;
    }

    // ── Events tab ──────────────────────────────────────────────────────────

    public void loadEventsForCurrentCompany() {
        try {
            view.showEvents(companyService.getAllEventsByCompany(companyId));
        } catch (RuntimeException ex) {
            view.showEventsError("Couldn't load events: " + ex.getMessage());
        }
    }

    /** Return the name for a given event UUID (falls back to short UUID on error). */
    public String getEventName(UUID eventId) {
        try {
            return companyService.getAllEventsByCompany(companyId).stream()
                    .filter(e -> eventId.equals(e.id))
                    .map(e -> e.name)
                    .findFirst()
                    .orElse(eventId.toString().substring(0, 8));
        } catch (RuntimeException ex) {
            return eventId.toString().substring(0, 8);
        }
    }

    // ── Roles tab ───────────────────────────────────────────────────────────

    public void loadRolesForCurrentCompany() {
        try {
            CompanyRolesViewDTO roles = companyService.viewRolesAndPermissions(token, companyId);
            view.showRoles(roles);
        } catch (RuntimeException ex) {
            view.showRolesError("Couldn't load roles: " + ex.getMessage());
        }
    }

    public void appointOwner(String memberId) {
        try {
            companyService.appointAdditionalOwner(token, companyId, memberId);
            view.onRoleMutationSucceeded("Done");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Appoint a manager with the selected permission set (II.4.7). */
    public void appointManager(String memberId, Set<CompanyPermission> permissions) {
        try {
            companyService.appointManager(token, companyId, memberId, permissions);
            view.onRoleMutationSucceeded("Manager appointed");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Replace a manager's permission set with the supplied one (II.4.11). */
    public void modifyManagerPermissions(String memberId, Set<CompanyPermission> permissions) {
        try {
            companyService.modifyManagerPermissions(token, companyId, memberId, permissions);
            view.onRoleMutationSucceeded("Permissions updated");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    public void removeManager(String memberId) {
        try {
            companyService.removeManagerAppointment(token, companyId, memberId);
            view.onRoleMutationSucceeded("Manager removed");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Remove an owner this owner previously appointed (II.4.9). */
    public void removeOwner(String memberId) {
        try {
            companyService.removeOwnerAppointment(token, companyId, memberId);
            view.onRoleMutationSucceeded("Owner removed");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Current owner resigns from ownership (II.4.10). Leaves the company afterwards. */
    public void resignOwnership() {
        try {
            companyService.resignOwnership(token, companyId);
            view.onLeftCompany("You resigned from ownership");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Delete the whole company (owner only). Leaves the company afterwards. */
    public void deleteCompany() {
        try {
            companyService.deleteCompany(token, companyId);
            view.onLeftCompany("Company deleted");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    public boolean isCurrentCompanyOpen() {
        try {
            return companyService.isCompanyOpen(companyId);
        } catch (RuntimeException ex) {
            return true; // assume open on lookup failure
        }
    }

    /** Suspend / close the company (II.4.13). */
    public void closeCompany() {
        try {
            boolean changed = companyService.closeCompany(token, companyId);
            view.onRoleMutationSucceeded(changed ? "Company suspended" : "Company was already suspended");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Reopen the company (II.4.14). */
    public void reopenCompany() {
        try {
            boolean changed = companyService.reopenCompany(token, companyId);
            view.onRoleMutationSucceeded(changed ? "Company reopened" : "Company was already open");
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** The currently bound member's id, or {@code null} if it can't be resolved. */
    public String getCurrentMemberId() {
        try {
            return userService.getMemberByToken(token).getMemberId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // ── Complaints tab (company-side handling, parallel to admin) ────────────

    /** Load complaints targeting this company for owners/managers to handle. */
    public void loadComplaints() {
        try {
            view.showComplaints(complaintService.getCompanyComplaints(token, companyId));
        } catch (RuntimeException ex) {
            view.showComplaintsError("Couldn't load complaints: " + ex.getMessage());
        }
    }

    /** Owner/manager responds to a company complaint (optionally resolving it). */
    public void respondToComplaint(UUID complaintId, String response, boolean resolve) {
        try {
            complaintService.companyRespondToComplaint(token, companyId, complaintId, response, resolve);
            view.showSuccess(resolve ? "Complaint resolved" : "Response sent");
            loadComplaints();
        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    /** Hands the view a fresh sales report for the current company (II.4.6). */
    public void loadSalesReport() {
        try {
            view.showSalesReport(companyService.getSalesReport(token, companyId));
        } catch (RuntimeException ex) {
            view.showError("Couldn't load sales report: " + ex.getMessage());
        }
    }

    // ── Policies tab ────────────────────────────────────────────────────────

    public void addDiscountRule(DiscountRule rule) {
        try {
            policyService.addDiscountRuleToCompany(token, companyId, rule);
            view.showSuccess("Discount rule added");
        } catch (RuntimeException ex) {
            view.showError("Couldn't add: " + ex.getMessage());
        }
    }

    public void addPurchaseRule(PurchaseRule rule) {
        try {
            policyService.addPurchaseRuleToCompany(token, companyId, rule);
            view.showSuccess("Purchase rule added");
        } catch (RuntimeException ex) {
            view.showError("Couldn't add: " + ex.getMessage());
        }
    }

   
    public List<Policy> getPoliciesForCompany() {
        try {
            return policyService.getPoliciesForCompany(companyId);
        } catch (RuntimeException ex) {
            view.showError("Could not load company policies: " + ex.getMessage());
            return List.of();
        }
    }

    public void setDiscountRulesForCompany(List<DiscountRule> rules, boolean isAdditive) {
        try {
            policyService.setDiscountRulesForCompany(token, companyId, rules, isAdditive);
            view.showSuccess("Discount policy saved");
        } catch (RuntimeException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public void setPurchaseRulesForCompany(List<PurchaseRule> rules, PurchasePolicy.Operator op) {
        try {
            policyService.setPurchaseRulesForCompany(token, companyId, rules, op);
            view.showSuccess("Purchase policy saved");
        } catch (RuntimeException ex) {
            view.showError("Error: " + ex.getMessage());
        }
    }

    public String getMemberIdByUsername(String username) {
        return userService.getMemberByUsername(username).getMemberId();
    }

    public String getMemberDisplayName(String memberId) {
        try { return userService.getMemberById(memberId).getUsername(); }
        catch (Exception e) { return memberId; }
    }

    public String getCurrentCompanyName() {
        if (companyId == null) return "Company";
        try {
            for (com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO dto
                    : companyService.getActiveCompanies()) {
                if (dto.getCompanyId().equals(companyId)) return dto.getCompanyName();
            }
        } catch (RuntimeException ignored) {}
        return "Company";
    }

    // ── Small row type shared with the view ─────────────────────────────────

    /** A single row of the "my companies" table in the chooser. */
    public static final class CompanyRow {
        public final UUID companyId;
        public final String name;
        public final String role;
        CompanyRow(UUID companyId, String name, String role) {
            this.companyId = companyId;
            this.name = name;
            this.role = role;
        }
    }
}
