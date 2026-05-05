package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;

/**
 * Marker type extending {@link IPolicyRepo}. Existing tests reference this
 * type by name (for {@code excludeFilters} on JPA scanning) so we keep it
 * around, but it no longer extends Spring Data's {@code JpaRepository} — the
 * Policy domain classes are not yet annotated as JPA entities, which would
 * make {@code JpaRepository<Policy, Integer>} fail at context-load time with
 * "Not a managed type: Policy".
 *
 * The active bean implementing {@link IPolicyRepo} is
 * {@link InMemoryPolicyRepo} — a no-op in-memory stub that lets the rest of
 * the application context (User / Event / Ticket / Order wiring) come up.
 * When the Policy team finishes annotating their entities, restore this file
 * to its JPA form and remove {@link InMemoryPolicyRepo}.
 */
public interface PolicyRepository extends IPolicyRepo {
}
