package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link com.sdnah.Ticket_Management_System_.Frontend.CompanyCreationView}.
 * Owns all business logic for creating a new company and its optional initial purchase policy.
 */
@Component
@UIScope
public class CompanyCreationPresenter {

    private final company_managment_serivce companyService;
    private final PolicyService             policyService;
    private View view;

    public CompanyCreationPresenter(company_managment_serivce companyService,
                                    PolicyService policyService) {
        this.companyService = companyService;
        this.policyService  = policyService;
    }

    public void setView(View view) {
        this.view = view;
    }

    // =========================================================================
    // handleCreate
    // =========================================================================

    /**
     * Validates inputs, creates the company, and optionally attaches a purchase policy.
     *
     * @param companyName   display name for the company (required)
     * @param useOrOperator true → OR logic between rules, false → AND
     * @param minAge        minimum buyer age (null = no restriction)
     * @param minTickets    minimum tickets per purchase (null = no restriction)
     * @param maxTickets    maximum tickets per purchase (null = no restriction)
     */
    public void handleCreate(String companyName,
                             boolean useOrOperator,
                             Integer minAge,
                             Integer minTickets,
                             Integer maxTickets) {

        // ── Validation ────────────────────────────────────────────────────────
        if (companyName == null || companyName.isBlank()) {
            view.showError("Company name is required");
            return;
        }
        if (minTickets != null && maxTickets != null && minTickets > maxTickets) {
            view.showError("Minimum tickets cannot be greater than maximum tickets");
            return;
        }

        // ── Session ───────────────────────────────────────────────────────────
        Object tokenObj = view.getSessionAttribute("token");
        if (tokenObj == null) {
            view.showWarning("Not logged in — sign in first to create a company");
            return;
        }
        String token = tokenObj.toString();

        // ── Create company ────────────────────────────────────────────────────
        try {
            UUID newCompanyId = companyService.openCompany(token, companyName.trim());

            // ── Optional purchase policy ──────────────────────────────────────
            List<PurchaseRule> rules = new ArrayList<>();
            if (minAge     != null && minAge     >= 0) rules.add(new MinAgeRule(minAge));
            if (minTickets != null && minTickets  > 0) rules.add(new MinTicketsRule(minTickets));
            if (maxTickets != null && maxTickets  > 0) rules.add(new MaxTicketsRule(maxTickets));

            if (!rules.isEmpty()) {
                PurchasePolicy.Operator op = useOrOperator
                        ? PurchasePolicy.Operator.OR
                        : PurchasePolicy.Operator.AND;
                try {
                    policyService.setPurchaseRulesForCompany(token, newCompanyId, rules, op);
                } catch (RuntimeException ex) {
                    // Policy creation failure is non-fatal — company was created successfully
                    view.showWarning("Company created, but purchase policy could not be saved: "
                            + ex.getMessage());
                }
            }

            view.setSessionAttribute("managingCompanyId", newCompanyId);
            view.showSuccess("Company \"" + companyName.trim() + "\" created successfully!");
            view.navigateToCompany();

        } catch (RuntimeException ex) {
            view.showError(ex.getMessage());
        }
    }

    // =========================================================================
    // View contract
    // =========================================================================

    public interface View {
        Object getSessionAttribute(String key);
        void   setSessionAttribute(String key, Object value);
        void   showSuccess(String message);
        void   showError(String message);
        void   showWarning(String message);
        void   navigateToCompany();
    }
}
