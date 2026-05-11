package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

/**
 * Immutable result of evaluating a PurchaseRule.
 * Carries a human-readable denial message shown to the buyer in the UI.
 */
public final class RuleResult {

    private final boolean allowed;
    private final String  message;

    private RuleResult(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public static RuleResult allowed() {
        return new RuleResult(true, "Purchase allowed.");
    }

    public static RuleResult denied(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Denial reason must not be blank");
        return new RuleResult(false, reason);
    }

    public boolean isAllowed() { return allowed; }
    public String  getMessage(){ return message; }

    @Override
    public String toString() {
        return (allowed ? "[ALLOWED] " : "[DENIED] ") + message;
    }
}