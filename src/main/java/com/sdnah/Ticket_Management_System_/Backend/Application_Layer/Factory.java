package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.*;

@Component
public class Factory {

    // ==================== Inject ALL services ====================
    private final UserService userService;
    private final company_managment_serivce companyService;
    private final TicketService ticketService;
    private final ActiveOrderService orderService;
    private final PolicyService policyService;
    private final EventService eventService;
    private final Waiting_QueueService waitingQueueService;
    private final SystemAdminService systemAdminService;
    private final AuthTokenService authTokenService;
    private final CompanyRoleService companyRoleService;
    private final VerificationEmail verificationService;

    // ==================== Inject repositories (if needed) ====================
    private final UserRepository userRepository;

    public Factory(
            UserService userService,
            company_managment_serivce companyService,
            TicketService ticketService,
            ActiveOrderService orderService,
            PolicyService policyService,
            EventService eventService,
            Waiting_QueueService waitingQueueService,
            SystemAdminService systemAdminService,
            AuthTokenService authTokenService,
            CompanyRoleService companyRoleService,
            VerificationEmail verificationService,
            UserRepository userRepository
    ) {
        this.userService = userService;
        this.companyService = companyService;
        this.ticketService = ticketService;
        this.orderService = orderService;
        this.policyService = policyService;
        this.eventService = eventService;
        this.waitingQueueService = waitingQueueService;
        this.systemAdminService = systemAdminService;
        this.authTokenService = authTokenService;
        this.companyRoleService = companyRoleService;
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    // ==================== STARTUP INITIALIZATION ====================
    @PostConstruct
    public void init() {
        System.out.println("========== SYSTEM STARTUP ==========");

        try {
            initializeSystem();
            System.out.println("========== SYSTEM READY ==========");
        } catch (Exception e) {
            System.err.println("❌ SYSTEM INIT FAILED");
            e.printStackTrace();
        }
    }

    private void initializeSystem() {

        System.out.println("🔧 Step 1: Verifying dependencies...");
        checkDependencies();

        System.out.println("🔧 Step 2: Initializing test data...");
        initTestData();

        System.out.println("🔧 Step 3: System configuration complete.");
    }

    private void checkDependencies() {
        if (userService == null ||
            authTokenService == null ||
            verificationService == null) {

            throw new RuntimeException("Critical services not initialized");
        }

        System.out.println("   ✓ All core services injected");
    }

    // ==================== OPTIONAL: TEST DATA ====================
    private void initTestData() {
        try {
            if (userRepository.count() == 0) {
                System.out.println("   → Creating default test user...");

                userService.register(
                        "testuser",
                        "123456",
                        "test@mail.com",
                        "0501234567",
                        com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod.EMAIL
                );

                // Auto-verify so the user can log in immediately without the email flow
                userRepository.findByUsername("testuser").ifPresent(member -> {
                    String code = member.getVerificationCode();
                    if (code != null) {
                        userService.verifyAccount("testuser", code);
                    }
                });

                System.out.println("   ✓ Test user ready  →  username: testuser   password: 123456");
            } else {
                System.out.println("   ✓ Users already exist");
            }

        } catch (Exception e) {
            System.out.println("   ⚠ Test data skipped: " + e.getMessage());
        }
    }
}