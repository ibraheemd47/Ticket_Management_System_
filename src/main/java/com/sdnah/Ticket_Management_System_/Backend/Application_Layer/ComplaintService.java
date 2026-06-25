package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CreateComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ComplaintStatus;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final UserService userService;
    private final KeyedLock keyedLock;
    private static final String LOCK_COMPLAINT_ID = "complaint";

    public ComplaintService(ComplaintRepository complaintRepository,
            UserService userService, KeyedLock lock) {
        this.complaintRepository = complaintRepository;
        this.userService = userService;
        this.keyedLock = lock;
    }

    /**
     * Create a new complaint. The user must be authenticated to create a complaint.
     * 
     * @param token             The authentication token of the user creating the
     *                          complaint.
     * @param complaint_request The details of the complaint to be created,
     *                          including subject, description, target type, and
     *                          target ID.
     * @return The UUID of the created complaint.
     */
    @Transactional
    public UUID createComplaint(String token, CreateComplaintDTO complaint_request) {
        String user_id = userService.requireMemberId(token);
        Complaint complaint = new Complaint(
                user_id,
                complaint_request.getSubject(),
                complaint_request.getDescription(),
                complaint_request.getTargetType(),
                complaint_request.getTargetId());
        complaintRepository.save(complaint);
        return complaint.getComplaintId();
    }

    /**
     * Retrieve all complaints reported by the authenticated user.
     * 
     * @param token The authentication token of the user whose complaints are to be
     *              retrieved.
     * @return A list of ComplaintDTOs representing the complaints reported by the
     *         user.
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getUserComplaints(String token) {
        String member_id = userService.requireMemberId(token);
        return complaintRepository.findByReporterMemberId(member_id)
                .stream()
                .map(ComplaintDTO::new)
                .toList();
    }

    /**
     * Retrieve all complaints with a specific status. This operation requires the
     * user to have admin privileges.
     * 
     * @param token  The authentication token of the admin user requesting the
     *               complaints.
     * @param status The status of the complaints to retrieve.
     * @return A list of ComplaintDTOs representing the complaints with the
     *         specified status.
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getComplaintsByStatus(String token, ComplaintStatus status) {
        userService.requireAdmin(token);
        return complaintRepository.findByStatus(status)
                .stream()
                .map(ComplaintDTO::new)
                .toList();
    }

    /**
     * Mark a complaint as "In Progress". This operation requires the user to have
     * admin privileges.
     * 
     * @param token       The authentication token of the admin user marking the
     *                    complaint as in progress.
     * @param complaintId The UUID of the complaint to be marked as in progress.
     */
    @Transactional
    public void markInProgress(String token, UUID complaintId) {
        userService.requireAdmin(token);

        keyedLock.callLocked(LOCK_COMPLAINT_ID, complaintId.toString(), () -> {
            Complaint complaint = complaintRepository.findById(complaintId)
                    .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

            complaint.markInProgress();
            complaintRepository.save(complaint);
            return null;
        });
    }

    /**
     * Resolve a complaint. This operation requires the user to have admin
     * privileges.
     * 
     * @param token         The authentication token of the admin user resolving the
     *                      complaint.
     * @param complaintId   The UUID of the complaint to be resolved.
     * @param adminResponse The response from the admin regarding the resolution.
     */
    @Transactional
    public void resolveComplaint(String token, UUID complaintId, String adminResponse) {
        userService.requireAdmin(token);

        keyedLock.callLocked(LOCK_COMPLAINT_ID, complaintId.toString(), () -> {
            Complaint complaint = complaintRepository.findById(complaintId)
                    .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

            complaint.resolve(adminResponse);
            complaintRepository.save(complaint);
            return null;
        });
    }

    /**
     * Reject a complaint. This operation requires the user to have admin
     * privileges.
     * 
     * @param token         The authentication token of the admin user rejecting the
     *                      complaint.
     * @param complaintId   The UUID of the complaint to be rejected.
     * @param adminResponse The response from the admin regarding the rejection.
     */
    @Transactional
    public void rejectComplaint(String token, UUID complaintId, String adminResponse) {
        userService.requireAdmin(token);

        keyedLock.callLocked(LOCK_COMPLAINT_ID, complaintId.toString(), () -> {
            Complaint complaint = complaintRepository.findById(complaintId)
                    .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

            complaint.reject(adminResponse);
            complaintRepository.save(complaint);
            return null;
        });
    }

    // ── Company-side handling (runs in parallel with the admin flow) ──────────

    private static final String TARGET_COMPANY = "COMPANY";

    /**
     * Complaints aimed at a company ({@code targetType="COMPANY"},
     * {@code targetId=companyId}). Only the company's owners/managers may view.
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getCompanyComplaints(String token, UUID companyId) {
        requireCompanyManager(token, companyId);
        return complaintRepository
                .findByTargetTypeIgnoreCaseAndTargetId(TARGET_COMPANY, companyId.toString())
                .stream()
                .map(ComplaintDTO::new)
                .toList();
    }

    /**
     * A company owner/manager responds to a complaint targeting their company.
     * Independent of the admin flow — stores a separate company response and,
     * when {@code resolve} is set, marks the complaint resolved.
     */
    @Transactional
    public void companyRespondToComplaint(String token, UUID companyId, UUID complaintId,
            String response, boolean resolve) {
        requireCompanyManager(token, companyId);
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Response is required");
        }
        keyedLock.callLocked(LOCK_COMPLAINT_ID, complaintId.toString(), () -> {
            Complaint complaint = complaintRepository.findById(complaintId)
                    .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

            if (!TARGET_COMPANY.equalsIgnoreCase(complaint.getTargetType())
                    || !companyId.toString().equals(complaint.getTargetId())) {
                throw new SecurityException("This complaint does not target your company");
            }

            complaint.companyRespond(response.trim(), resolve);
            complaintRepository.save(complaint);
            return null;
        });
    }

    private Member requireCompanyManager(String token, UUID companyId) {
        Member actor = userService.getMemberByToken(token);
        if (!(actor.isOwnerInCompany(companyId) || actor.isManagerInCompany(companyId))) {
            throw new SecurityException("Only the company's owners or managers can handle its complaints");
        }
        return actor;
    }

}
