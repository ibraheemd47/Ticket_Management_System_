package com.sdnah.Ticket_Management_System_.Backend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("dev")
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    public DemoDataLoader(UserRepository userRepository,
                          PasswordHasher passwordHasher,
                          NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        createDemoUserIfMissing();
    }

    private void createDemoUserIfMissing() {
        String username = "zaz";
        String password = "123456";
        String email = "zaz@test.com";
        String phone = "0500000000";

        if (userRepository.existsByUsername(username)) {
            return;
        }

        String memberId = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(password);

        Member member = new Member(memberId, username, passwordHash);
        member.setEmail(email);
        member.setPhone(phone);
        member.setVerified(true);
        member.logout();

        userRepository.save(member);

        notificationService.createNotification(
                memberId,
                "Welcome zaz! This is your first demo notification.",
                NotificationType.ROLE_CHANGED
        );

        notificationService.createNotification(
                memberId,
                "Demo notification system is working.",
                NotificationType.ROLE_CHANGED
        );

        System.out.println("Demo user created:");
        System.out.println("username = " + username);
        System.out.println("password = " + password);
        System.out.println("memberId = " + memberId);
    }
}