package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CreateComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ComplaintStatus;
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

}
