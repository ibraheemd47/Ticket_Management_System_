package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link com.sdnah.Ticket_Management_System_.Frontend.CompanyCreationView}.
 * Owns all business logic for creating a new company.
 */
@Component
@UIScope
public class CompanyCreationPresenter {

    private final company_managment_serivce companyService;
    private View view;

    public CompanyCreationPresenter(company_managment_serivce companyService) {
        this.companyService = companyService;
    }

    public void setView(View view) {
        this.view = view;
    }

    // =========================================================================
    // handleCreate
    // =========================================================================

    public void handleCreate(Integer companyId, String companyName) {
        // ── Validation ────────────────────────────────────────────────────────
        if (companyId == null) {
            view.showError("Company ID is required");
            return;
        }
        if (companyId <= 0) {
            view.showError("Company ID must be positive");
            return;
        }
        if (companyName == null || companyName.isBlank()) {
            view.showError("Company name is required");
            return;
        }

        // ── Session ───────────────────────────────────────────────────────────
        Object tokenObj = view.getSessionAttribute("token");
        if (tokenObj == null) {
            view.showWarning("Not logged in — sign in first to create a company");
            return;
        }
        String token = tokenObj.toString();

        // ── Create ────────────────────────────────────────────────────────────
        try {
            UUID newCompanyId = companyService.openCompany(token, companyName.trim());
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
