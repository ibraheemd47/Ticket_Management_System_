package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.SuspensionDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.UserDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

@Service
public class SystemAdminService {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdminService.class);

    private final UserRepository userRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final PurchaseRepository purchaseRepository;
    private final Waiting_QueueRepository waitingQueueRepository;

    private final UserService userService;
    private final KeyedLock keyedLock;

    private final ComplaintRepository complaintRepository;
    private final ComplaintService complaintService;

    private static final String LOCK_NS = "system-admin:member";

    public SystemAdminService(UserRepository userRepository,
            SystemAdminRepository systemAdminRepository,
            PurchaseRepository purchaseRepository,
            Waiting_QueueRepository waitingQueueRepository,
            UserService userService,
            ComplaintRepository complaintRepository,
            ComplaintService complaintService,
            KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.purchaseRepository = purchaseRepository;
        this.waitingQueueRepository = waitingQueueRepository;
        this.userService = userService;
        this.complaintRepository = complaintRepository;
        this.complaintService = complaintService;
        this.keyedLock = keyedLock;
    }

    @Transactional
    public void assign_system_admin(String token, String target_member_id) {
        logger.info("Assign system admin requested for targetMemberId={}", target_member_id);

        Member adminActor = requireAdmin(token);

        keyedLock.runLocked(LOCK_NS, target_member_id, () -> {
            Member to_assign = userRepository.findByMemberId(target_member_id);
            if (to_assign == null) {
                throw new IllegalArgumentException("Target member not found");
            }
            if (is_admin(target_member_id)) {
                throw new IllegalArgumentException("Member is already an admin");
            }

            System_admin new_admin = new System_admin(to_assign, adminActor.getMemberId());

            userRepository.delete(to_assign);
            userRepository.flush();
            systemAdminRepository.save(new_admin);
        });
    }

    public Member requireAdmin(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        Member admin = userService.getMemberByToken(token);

        if (!admin.isSystemAdmin()) {
            throw new IllegalArgumentException("Only system admins can perform this action");
        }

        return admin;
    }

    private boolean is_admin(String memberId) {
        return systemAdminRepository.existsByMemberId(memberId);
    }

    public void close_company(String token, String company_id) {
        requireAdmin(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }

    public void open_company(String token, String company_id) {
        requireAdmin(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }

    // version 2 - member suspension/reactivation
    // use case ( II.6.7 )
    @Transactional
    public void suspendUser(String token, String targetMemberId, long durationHours) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null)
                throw new IllegalArgumentException("Member not found");
            if (target.isSystemAdmin())
                throw new IllegalArgumentException("Cannot suspend a system admin");

            LocalDateTime until = LocalDateTime.now().plusHours(durationHours);
            target.suspend(until);
            checkIsloggedInAndLogout(target);
            // todo: notify user of suspension and duration
            // notifier.notifyUser(targetMemberId,"Your account has been suspended until " +
            // until);
        });
    }

    // use case ( II.6.7 )
    @Transactional
    public void suspendPermanently(String token, String targetMemberId) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null)
                throw new IllegalArgumentException("Member not found");
            if (target.isSystemAdmin())
                throw new IllegalArgumentException("Cannot suspend a system admin");

            target.suspendPermanently();
            checkIsloggedInAndLogout(target);
            // todo: notify user of permanent suspension
            // notifier.notifyUser(targetMemberId,"Your account has been suspended
            // permanently.");
        });
    }

    // in case the user is currently logged in, we log them out
    private void checkIsloggedInAndLogout(Member target) {
        if (target.isLoggedin()) {
            target.logout();
        }
        userRepository.save(target);
    }

    // use case ( II.6.8 )
    @Transactional
    public void unsuspendUser(String token, String targetMemberId) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null)
                throw new IllegalArgumentException("Member not found");
            if (!target.isSuspended())
                throw new IllegalArgumentException("Member is not suspended");

            target.unsuspend();
            userRepository.save(target);
            // todo: notify user of reactivation
            // notifier.notifyUser(targetMemberId, "Your suspension has been lifted.");
        });
    }

    // use case ( II.6.9 )
    public List<SuspensionDTO> getSuspensions(String token) {
        requireAdmin(token);
        return userRepository.findAll().stream()
                .filter(Member::isSuspended)
                .map(m -> new SuspensionDTO(
                        m.getMemberId(),
                        m.getUsername(),
                        m.getSuspensionStartedAt(),
                        m.getSuspendedUntil(),
                        m.isSuspendedPermanently()))
                .collect(Collectors.toList());
    }

    // for ui
    /**
     * Retrieves a list of all users in the system. This method is intended for system
     * @param token The authentication token of the system admin requesting the user list. Must be a valid token associated with a system admin account.
     * @return A list of UserDTO objects representing all users in the system. Each UserDTO contains details about a user, such as their member ID, username, and other relevant information. This allows the system admin to view and manage the users in the ticket management system effectively.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(String token) {
        requireAdmin(token);
        return userRepository.findAll().stream().map(Member::toUserDTO).collect(Collectors.toList());
    }

    /**
     * Retrieves the count of currently logged-in users in the system. This method is
     * @param token The authentication token of the system admin requesting the count. Must be a valid token associated with a system admin account.
     * @return The number of users currently logged in to the system. This count includes all members who have an active session and are considered logged in at the time of the request.
     */
    @Transactional(readOnly = true)
    public int getLoggedInUsersCount(String token) {
        requireAdmin(token);
        return (int) userRepository.findAll().stream()
                .filter(Member::isLoggedin)
                .count();
    }

    /**
     * Removes a member from the system. 
     * @param token The authentication token of the system admin requesting the member removal. Must be a valid token associated with a system admin account.
     * @param usernameOrMemberId The username or member ID of the member to be removed. Must correspond to an existing member in the system.
     */
    @Transactional
    public void removeMember(String token, String usernameOrMemberId) {
        requireAdmin(token);

        Member target = findMemberByUsernameOrMemberId(usernameOrMemberId);

        if (target == null) {
            throw new IllegalArgumentException("Member not found");
        }

        if (target.isSystemAdmin()) {
            throw new IllegalArgumentException("Cannot remove a system admin");
        }

        keyedLock.runLocked(LOCK_NS, target.getMemberId(), () -> {
            Member freshTarget = userRepository.findByMemberId(target.getMemberId());

            if (freshTarget == null) {
                throw new IllegalArgumentException("Member not found");
            }

            if (freshTarget.isSystemAdmin()) {
                throw new IllegalArgumentException("Cannot remove a system admin");
            }

            userRepository.delete(freshTarget);
        });
    }

    private Member findMemberByUsernameOrMemberId(String usernameOrMemberId) {
        if (usernameOrMemberId == null || usernameOrMemberId.isBlank()) {
            throw new IllegalArgumentException("Username/memberId is required");
        }

        String cleanValue = usernameOrMemberId.trim();

        return userRepository.findByUsername(cleanValue)
                .orElseGet(() -> userRepository.findByMemberId(cleanValue));
    }

    /**
     * Retrieves a list of all waiting queues in the system. This method is intended
     * for system admins to monitor and manage the waiting queues in the ticket
     * management system. It returns a list of WaitingQueue objects representing all
     * the waiting queues currently present in the system, allowing the admin to
     * view their details and status.
     * 
     * @param token The authentication token of the system admin requesting the
     *              waiting queues. Must be a valid token associated with a system
     *              admin account.
     * @return A list of WaitingQueue objects representing all waiting queues in the
     *         system. Each WaitingQueue object contains details about the queue,
     *         such as its unique identifier, name, current flow, and the number of
     *         users currently in the queue.
     */
    @Transactional(readOnly = true)
    public List<WaitingQueue> getAllQueues(String token) {
        requireAdmin(token);
        return waitingQueueRepository.findAll();
    }

    /**
     * Increases the flow of a waiting queue by a specified amount. This method is
     * intended for system admins to manage and control the flow of waiting queues
     * in the ticket management system. It increases the flow of the specified
     * waiting queue by the given amount, allowing for adjustments to the queue's
     * processing speed or capacity.
     * 
     * @param token   The authentication token of the system admin requesting the
     *                queue flow increase. Must be a valid token associated with a
     *                system admin account.
     * @param queueId The unique identifier of the waiting queue for which to
     *                increase the flow. Must correspond to an existing waiting
     *                queue in the system.
     * @param amount  The amount by which to increase the queue flow. Must be a
     *                positive integer representing the number of units to add to
     *                the current flow of the waiting queue.
     * @return The updated WaitingQueue object after the flow has been increased.
     *         This object reflects the new state of the waiting queue with the
     *         increased flow.
     * @throws IllegalArgumentException if the specified queueId does not correspond
     *                                  to an existing waiting
     */
    @Transactional
    public WaitingQueue increaseQueueFlow(String token, long queueId, int amount) {
        requireAdmin(token);

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        WaitingQueue queue = waitingQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        queue.increaseFlow(amount);

        return waitingQueueRepository.save(queue);
    }

    /**
     * Decreases the flow of a waiting queue by a specified amount. This method is
     * intended for system admins to manage and control the flow of waiting queues
     * in the ticket management system. It reduces the flow of the specified waiting
     * queue by the given amount, allowing for adjustments to the queue's processing
     * speed or capacity.
     * 
     * @param token   The authentication token of the system admin requesting the
     *                queue flow decrease. Must be a valid token associated with a
     *                system admin account.
     * @param queueId The unique identifier of the waiting queue for which to
     *                decrease the flow. Must correspond to an existing waiting
     *                queue in the system.
     * @param amount  The amount by which to decrease the queue flow. Must be a
     *                positive integer representing the number of units to reduce
     *                from the current flow of the waiting queue.
     * @return The updated WaitingQueue object after the flow has been decreased.
     *         This object reflects the new state of the waiting queue with the
     *         reduced flow.
     * @throws IllegalArgumentException if the specified queueId does not correspond
     *                                  to an existing waiting
     */
    @Transactional
    public WaitingQueue decreaseQueueFlow(String token, long queueId, int amount) {
        requireAdmin(token);

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        WaitingQueue queue = waitingQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        queue.decreaseFlow(amount);

        return waitingQueueRepository.save(queue);
    }

    /**
     * Clears all users from a waiting queue. This method is intended for system
     * admins to manage and reset waiting queues in the ticket management system. It
     * removes all users currently in the specified waiting queue, effectively
     * resetting it to an empty state.
     * 
     * @param token   The authentication token of the system admin requesting the
     *                queue clearance. Must be a valid token associated with a
     *                system admin account.
     * @param queueId The unique identifier of the waiting queue to be cleared. Must
     *                correspond to an existing waiting queue in the system.
     * @throws IllegalArgumentException if the specified queueId does not correspond
     *                                  to an existing waiting queue, or if the
     *                                  token is invalid or does not belong to a
     *                                  system admin.
     */
    @Transactional
    public void clearQueue(String token, long queueId) {
        requireAdmin(token);

        WaitingQueue queue = waitingQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        queue.clearQueue();

        waitingQueueRepository.save(queue);
    }

    /**
     * Retrieves a list of purchases made by a specific buyer. This method is
     * intended for system admins to monitor and manage purchases made by users in
     * the ticket management system.
     * 
     * @param token   The authentication token of the system admin requesting the
     *                purchases. Must be a valid token associated with a system
     *                admin account.
     * @param buyerId The unique identifier of the buyer for whom to retrieve
     *                purchases.
     * @return A list of Purchase objects representing the purchases made by the
     *         specified buyer.
     */
    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByEvent(String token, UUID eventId) {
        requireAdmin(token);

        if (eventId == null) {
            throw new IllegalArgumentException("Event id is required");
        }

        return purchaseRepository.findByEventId(eventId);
    }

    /**
     * Retrieves all complaints in the system. This method is intended for system
     * admins to monitor and manage user complaints.
     * 
     * @param token The authentication token of the system admin requesting the
     *              complaints. Must be a valid token associated with a system admin
     *              account.
     * @return A list of ComplaintDTO objects representing all complaints in the
     *         system. Each ComplaintDTO contains details about the complaint, such
     *         as the complainant, the subject of the complaint, the description,
     *         and the status.
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getAllComplaints(String token) {
        requireAdmin(token);
        return complaintService.getUserComplaints(token);
    }
}