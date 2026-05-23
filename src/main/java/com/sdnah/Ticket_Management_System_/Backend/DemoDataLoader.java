package com.sdnah.Ticket_Management_System_.Backend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("dev")
public class DemoDataLoader implements CommandLineRunner {

    private static final String DEMO_USERNAME = "zaz";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_EMAIL = "zaz@test.com";
    private static final String DEMO_PHONE = "0500000000";
    private static final UUID DEMO_COMPANY_ID = UUID.randomUUID();

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final IEventRepository eventRepository;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    public DemoDataLoader(UserRepository userRepository,
                          CompanyRepository companyRepository,
                          IEventRepository eventRepository,
                          PasswordHasher passwordHasher,
                          NotificationService notificationService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Member demoUser = createDemoUserIfMissing();

        Company demoCompany = createDemoCompanyIfMissing(demoUser);
        createDemoEventsIfMissing(demoUser, demoCompany);
        createDemoNotificationsIfMissing(demoUser);

        System.out.println("Demo data loaded:");
        System.out.println("username = " + DEMO_USERNAME);
        System.out.println("password = " + DEMO_PASSWORD);
        System.out.println("memberId = " + demoUser.getMemberId());
    }

    private Member createDemoUserIfMissing() {
        if (userRepository.existsByUsername(DEMO_USERNAME)) {
            return userRepository.findByUsername(DEMO_USERNAME)
                    .orElseThrow(() -> new IllegalStateException("Demo user exists but could not be loaded"));
        }

        String memberId = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(DEMO_PASSWORD);

        Member member = new Member(memberId, DEMO_USERNAME, passwordHash);
        member.setEmail(DEMO_EMAIL);
        member.setPhone(DEMO_PHONE);
        member.setVerified(true);
        member.setActive(true);
        member.logout();

        return userRepository.save(member);
    }

    private Company createDemoCompanyIfMissing(Member demoUser) {
        if (companyRepository.existsById(DEMO_COMPANY_ID)) {
            return companyRepository.findById(DEMO_COMPANY_ID)
                    .orElseThrow(() -> new IllegalStateException("Demo company exists but could not be loaded"));
        }

        Company company = new Company(
                "Demo Concerts Company",
                demoUser.getMemberId()
        );

        company = companyRepository.save(company);

        demoUser.addCompanyRole(new CompanyRoleAssignment(
                company.getCompanyId(),
                demoUser.getMemberId(),
                CompanyRoleType.OWNER,
                Set.of()
        ));

        userRepository.save(demoUser);

        return company;
    }

    private void createDemoEventsIfMissing(Member demoUser, Company company) {
        if (!company.getAssociatedEventIds().isEmpty()) {
            return;
        }

        Event event1 = new Event(
                "Demo Rock Concert",
                show_type.PERFORMANCE,
                DEMO_COMPANY_ID,
                Long.valueOf(1)
        );

        event1.editDescription("A demo rock concert used for frontend testing.", Long.valueOf(1));
        event1.editVenue("Demo Arena", Long.valueOf(1));
        event1.editDates(daysFromNow(7), daysFromNow(7), Long.valueOf(1));

        Event event2 = new Event(
                "Demo Tech Conference",
                show_type.CONFERENCE,
                DEMO_COMPANY_ID,
                Long.valueOf(1)
        );

        event2.editDescription("A demo technology conference used for frontend testing.", Long.valueOf(1));
        event2.editVenue("Demo Convention Center", Long.valueOf(1));
        event2.editDates(daysFromNow(14), daysFromNow(14), Long.valueOf(1));

        Event savedEvent1 = eventRepository.save(event1);
        Event savedEvent2 = eventRepository.save(event2);

        company.addEventId(demoUser.getMemberId(), savedEvent1.getEventId());
        company.addEventId(demoUser.getMemberId(), savedEvent2.getEventId());

        companyRepository.save(company);
    }

    private void createDemoNotificationsIfMissing(Member demoUser) {
        if (!notificationService.getNotificationsForUser(demoUser.getMemberId()).isEmpty()) {
            return;
        }

        notificationService.createNotification(
                demoUser.getMemberId(),
                "Welcome zaz! This is your first demo notification.",
                NotificationType.GENERIC
        );

        notificationService.createNotification(
                demoUser.getMemberId(),
                "Demo notification system is working.",
                NotificationType.SYSTEM_ANNOUNCEMENT
        );

        notificationService.createNotification(
                demoUser.getMemberId(),
                "You are the owner of Demo Concerts Company.",
                NotificationType.OWNER_APPOINTED
        );
    }

    private Date daysFromNow(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}