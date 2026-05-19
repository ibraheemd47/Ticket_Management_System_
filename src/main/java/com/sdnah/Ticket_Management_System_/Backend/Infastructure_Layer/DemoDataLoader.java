package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.UserRole;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.AssertingParty.Verification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements CommandLineRunner {

        private final UserRepository userRepository;
        private final CompanyRepository companyRepository;
        private final IEventRepository eventRepository;
        private final NotificationRepository notificationRepository;
        VerificationMethod verificationMethod = VerificationMethod.EMAIL;

        private UserService userService;

        public DemoDataLoader(UserService userService,
                        UserRepository userRepository,
                        CompanyRepository companyRepository,
                        IEventRepository eventRepository,
                        NotificationRepository notificationRepository) {
                this.userRepository = userRepository;
                this.companyRepository = companyRepository;
                this.eventRepository = eventRepository;
                this.notificationRepository = notificationRepository;
                this.userService = userService;
        }

        @Override
        @Transactional
        public void run(String... args) {
                // Prevent duplicate demo data on every restart
                if (userRepository.existsByUsername("demo-buyer")) {
                        return;
                }

                createDemoUsers();
                createDemoCompaniesAndEvents();
                createDemoNotifications();
        }

        private void createDemoUsers() {
                Member admin = new Member("demo-admin", "admin", "admin123");
                admin.setActive(true);
                admin.setVerified(true);
                admin.setRole(UserRole.SYSTEM_ADMIN);

                userService.register("demo-admin", "Demo123", "exam@mail.com", "0545454545", verificationMethod);
                // userRepository.save(admin);
                userService.verifyAccount("demo-admin", "verfication-code-for-demo-admin");
                Member owner = new Member("demo-owner", "owner", "owner123");
                owner.setActive(true);
                owner.setVerified(true);
                userService.register("demo-owner", "Demo-Owner", "owner@mail.com", "0545454545", verificationMethod);
                // userRepository.save(owner);
                userService.verifyAccount("demo-owner", "verfication-code-for-demo-owner");
                Member manager = new Member("demo-manager", "manager", "manager123");
                manager.setActive(true);
                manager.setVerified(true);
                userService.register("demo-manager", "Demo Manager", "manager@mail.com", "0545454545",
                                verificationMethod);
                // userRepository.save(manager);
                userService.verifyAccount("demo-manager", "verfication-code-for-demo-manager");
                Member buyer = new Member("demo-buyer", "buyer", "buyer123");
                buyer.setActive(true);
                buyer.setVerified(true);
                userService.register("demo-buyer", "Demo-Buyer", "buyer@mail.com", "0545454545", verificationMethod);
                // userRepository.save(buyer);
                userService.verifyAccount("demo-buyer", "verfication-code-for-demo-buyer");
        }

        private void createDemoCompaniesAndEvents() {
                Company mainCompany = new Company(1, "Demo Concerts Company", "demo-owner");

                mainCompany.appointManager(
                                "demo-owner",
                                "demo-manager",
                                Set.of(
                                                CompanyPermission.MANAGE_EVENTS,
                                                CompanyPermission.VIEW_HISTORY,
                                                CompanyPermission.VIEW_ROLES));

                companyRepository.save(mainCompany);

                Member owner = userRepository.findByUsername("demo-owner")
                                .orElseThrow(() -> new IllegalStateException("Demo owner not found"));

                owner.addCompanyRole(new CompanyRoleAssignment(
                                1,
                                "demo-owner",
                                CompanyRoleType.OWNER,
                                Set.of()));
                userRepository.save(owner);

                Member manager = userRepository.findByUsername("demo-manager")
                                .orElseThrow(() -> new IllegalStateException("Demo manager not found"));

                manager.addCompanyRole(new CompanyRoleAssignment(
                                1,
                                "demo-owner",
                                CompanyRoleType.MANAGER,
                                Set.of()));
                userRepository.save(manager);

                Event event1 = new Event(
                                "Demo Rock Concert",
                                show_type.PERFORMANCE,
                                1L,
                                Long.valueOf(100));

                Event event2 = new Event(
                                "Demo Tech Conference",
                                show_type.CONFERENCE,
                                1L,
                                Long.valueOf(100));

                Event savedEvent1 = eventRepository.save(event1);
                Event savedEvent2 = eventRepository.save(event2);

                mainCompany.addEventId("demo-owner", savedEvent1.getEventId());
                mainCompany.addEventId("demo-owner", savedEvent2.getEventId());

                companyRepository.save(mainCompany);
        }

        private void createDemoNotifications() {
                notificationRepository.save(new Notification(
                                "buyer",
                                "Welcome! This is your first demo notification.",
                                NotificationType.GENERIC));

                notificationRepository.save(new Notification(
                                "buyer",
                                "Your purchase was completed successfully.",
                                NotificationType.PURCHASE_SUCCESS));

                notificationRepository.save(new Notification(
                                "manager",
                                "You were appointed as manager in Demo Concerts Company.",
                                NotificationType.MANAGER_APPOINTED));

                notificationRepository.save(new Notification(
                                "owner",
                                "Demo Concerts Company was created successfully.",
                                NotificationType.SYSTEM_ANNOUNCEMENT));
        }
}