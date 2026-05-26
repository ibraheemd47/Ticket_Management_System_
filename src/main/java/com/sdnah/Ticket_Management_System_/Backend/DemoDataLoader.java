package com.sdnah.Ticket_Management_System_.Backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

@Component
@Profile("dev")
public class DemoDataLoader implements CommandLineRunner {

        private static final String PASSWORD = "123456";


        private final UserRepository userRepository;
        private final SystemAdminRepository systemAdminRepository;
        private final CompanyRepository companyRepository;
        private final IEventRepository eventRepository;
        private final Waiting_QueueRepository waitingQueueRepository;
        private final PurchaseRepository purchaseRepository;
        private final ActiveOrderRepository activeOrderRepository;
        private final ComplaintRepository complaintRepository;
        private final PasswordHasher passwordHasher;
        private final NotificationService notificationService;

        public DemoDataLoader(UserRepository userRepository,
                        SystemAdminRepository systemAdminRepository,
                        CompanyRepository companyRepository,
                        IEventRepository eventRepository,
                        Waiting_QueueRepository waitingQueueRepository,
                        PurchaseRepository purchaseRepository,
                        ActiveOrderRepository activeOrderRepository,
                        ComplaintRepository complaintRepository,
                        PasswordHasher passwordHasher,
                        NotificationService notificationService) {

                this.userRepository = userRepository;
                this.systemAdminRepository = systemAdminRepository;
                this.companyRepository = companyRepository;
                this.eventRepository = eventRepository;
                this.waitingQueueRepository = waitingQueueRepository;
                this.purchaseRepository = purchaseRepository;
                this.activeOrderRepository = activeOrderRepository;
                this.complaintRepository = complaintRepository;
                this.passwordHasher = passwordHasher;
                this.notificationService = notificationService;
        }

        @Override
        @Transactional
        public void run(String... args) {
                System.out.println("========== Loading full DEV demo data ==========");

                Member admin = createSystemAdminIfMissing();
                Member owner = createMemberIfMissing(UUID.randomUUID().toString(), "owner_demo", "Owner", "Demo", "owner@test.com");
                Member coOwner = createMemberIfMissing(UUID.randomUUID().toString(), "co_owner_demo", "Co", "Owner",
                                "coowner@test.com");
                Member manager = createMemberIfMissing(UUID.randomUUID().toString(), "manager_demo", "Manager", "Demo",
                                "manager@test.com");
                Member limitedManager = createMemberIfMissing(UUID.randomUUID().toString(), "limited_manager_demo","Limited", "Manager", "limited@test.com");
                Member buyer = createMemberIfMissing(UUID.randomUUID().toString(), "buyer_demo", "Buyer", "Demo", "buyer@test.com");
                Member buyer2 = createMemberIfMissing(UUID.randomUUID().toString(), "buyer2_demo", "Second", "Buyer",
                                "buyer2@test.com");
                Member user = createMemberIfMissing(UUID.randomUUID().toString(), "user_demo", "RegularUser", "User", "user@test.com");
                Member suspended = createSuspendedMemberIfMissing();

                Company concerts = createCompanyIfMissing("Demo Concerts Company", owner);
                Company conferences = createCompanyIfMissing("Demo Conferences Company", owner);

                connectCompanyRoles(concerts, owner, coOwner, manager, limitedManager);
                connectCompanyRoles(conferences, owner, coOwner, manager, limitedManager);

                Event rockFestival = createEventIfMissing(
                                "Demo Rock Festival",
                                show_type.FESTIVAL,
                                concerts,
                                owner.getMemberId(),
                                manager.getMemberId(),
                                "Demo Arena",
                                "Large demo festival with multiple shows.");

                Event techConference = createEventIfMissing(
                                "Demo Tech Conference",
                                show_type.CONFERENCE,
                                conferences,
                                owner.getMemberId(),
                                manager.getMemberId(),
                                "Demo Convention Center",
                                "Technology conference demo event.");

                Event theaterShow = createEventIfMissing(
                                "Demo Theater Performance",
                                show_type.PERFORMANCE,
                                concerts,
                                owner.getMemberId(),
                                manager.getMemberId(),
                                "Demo Theater Hall",
                                "Performance demo event.");

                addShowsIfMissing(rockFestival);
                addShowsIfMissing(techConference);
                addShowsIfMissing(theaterShow);

                createQueuesIfMissing();

                createPurchasesIfMissing(buyer, buyer2, rockFestival, techConference);
                createActiveOrdersIfMissing(buyer2, theaterShow);

                createComplaintsIfMissing(buyer, buyer2, user, suspended);

                createNotificationsIfMissing(admin, owner, coOwner, manager, limitedManager, buyer, buyer2, user,
                                suspended);

                printCredentials();

                System.out.println("========== Full DEV demo data loaded ==========");
        }

        private Member createMemberIfMissing(String memberId,
                        String username,
                        String firstName,
                        String lastName,
                        String email) {

                return userRepository.findByUsername(username)
                                .orElseGet(() -> {
                                        Member member = new Member(memberId, username, passwordHasher.hash(PASSWORD));

                                        member.setFirstName(firstName);
                                        member.setLastName(lastName);
                                        member.setEmail(email);
                                        member.setPhone("0500000000");
                                        member.setAddress("Demo Street 1");
                                        member.setCity("Beer Sheva");
                                        member.setCountry("Israel");
                                        member.setBirthDate(LocalDate.of(2000, 1, 1));
                                        member.setAge(26);
                                        member.setVerified(true);
                                        member.setActive(true);
                                        member.logout();

                                        return userRepository.saveAndFlush(member);
                                });
        }

        private Member createSystemAdminIfMissing() {
                Optional<Member> existing = userRepository.findByUsername("admin_demo");

                if (existing.isPresent()) {
                        Member member = existing.get();

                        if (!systemAdminRepository.existsByMemberId(member.getMemberId())) {
                                throw new IllegalStateException(
                                                "User admin_demo exists but is not a System_admin. Delete this demo user or use a clean DB.");
                        }

                        return member;
                }

                Member baseAdmin = new Member(
                                UUID.randomUUID().toString(),
                                "admin_demo",
                                passwordHasher.hash(PASSWORD));

                baseAdmin.setFirstName("Admin");
                baseAdmin.setLastName("Demo");
                baseAdmin.setEmail("admin@test.com");
                baseAdmin.setPhone("0509999999");
                baseAdmin.setVerified(true);
                baseAdmin.setActive(true);
                baseAdmin.logout();

                System_admin admin = new System_admin(baseAdmin, "DemoDataLoader");

                return systemAdminRepository.saveAndFlush(admin);
        }

        private Member createSuspendedMemberIfMissing() {
                Member suspended = createMemberIfMissing(
                                UUID.randomUUID().toString(),
                                "suspended_demo",
                                "Suspended",
                                "User",
                                "suspended@test.com");

                if (!suspended.isSuspended()) {
                        suspended.suspend(LocalDateTime.now().plusDays(7));
                        suspended.logout();
                        userRepository.saveAndFlush(suspended);
                }

                return suspended;
        }

        private Company createCompanyIfMissing(String name, Member founder) {
                return companyRepository.findAll()
                                .stream()
                                .filter(company -> company.getCompanyName().equals(name))
                                .findFirst()
                                .orElseGet(() -> {
                                        Company company = new Company(name, founder.getMemberId());
                                        company.setLogoURL("https://example.com/demo-logo.png");
                                        company.updateRating(4.5);
                                        return companyRepository.saveAndFlush(company);
                                });
        }

        private void connectCompanyRoles(Company company,
                        Member owner,
                        Member coOwner,
                        Member manager,
                        Member limitedManager) {

                if (!company.isOwner(coOwner.getMemberId())) {
                        company.appointAdditionalOwner(owner.getMemberId(), coOwner.getMemberId());
                }

                if (!company.isManager(manager.getMemberId())) {
                        company.appointManager(
                                        owner.getMemberId(),
                                        manager.getMemberId(),
                                        Set.of(
                                                        CompanyPermission.MANAGE_EVENTS,
                                                        CompanyPermission.VIEW_HISTORY,
                                                        CompanyPermission.RESPOND_TO_INQUIRIES,
                                                        CompanyPermission.VIEW_ROLES));
                }

                if (!company.isManager(limitedManager.getMemberId())) {
                        company.appointManager(
                                        owner.getMemberId(),
                                        limitedManager.getMemberId(),
                                        Set.of(
                                                        CompanyPermission.VIEW_HISTORY,
                                                        CompanyPermission.VIEW_ROLES));
                }

                companyRepository.saveAndFlush(company);

                addMemberRoleIfMissing(
                                owner,
                                new CompanyRoleAssignment(
                                                company.getCompanyId(),
                                                owner.getMemberId(),
                                                CompanyRoleType.OWNER,
                                                Set.of()));

                addMemberRoleIfMissing(
                                coOwner,
                                new CompanyRoleAssignment(
                                                company.getCompanyId(),
                                                owner.getMemberId(),
                                                CompanyRoleType.OWNER,
                                                Set.of()));

                addMemberRoleIfMissing(
                                manager,
                                new CompanyRoleAssignment(
                                                company.getCompanyId(),
                                                owner.getMemberId(),
                                                CompanyRoleType.MANAGER,
                                                Set.of(
                                                                ManagerPermission.ADD_PRODUCT,
                                                                ManagerPermission.REMOVE_PRODUCT,
                                                                ManagerPermission.UPDATE_PRODUCT,
                                                                ManagerPermission.VIEW_PURCHASE_HISTORY,
                                                                ManagerPermission.REPLY_TO_MESSAGES)));

                addMemberRoleIfMissing(
                                limitedManager,
                                new CompanyRoleAssignment(
                                                company.getCompanyId(),
                                                owner.getMemberId(),
                                                CompanyRoleType.MANAGER,
                                                Set.of(
                                                                ManagerPermission.VIEW_STORE_INFO,
                                                                ManagerPermission.VIEW_PURCHASE_HISTORY)));
        }

        private void addMemberRoleIfMissing(Member member, CompanyRoleAssignment assignment) {
                boolean exists = member.getCompanyRoles()
                                .stream()
                                .anyMatch(role -> role.getCompanyId().equals(assignment.getCompanyId())
                                                && role.getRoleType() == assignment.getRoleType());

                if (!exists) {
                        member.addCompanyRole(assignment);
                        userRepository.saveAndFlush(member);
                }
        }

        private Event createEventIfMissing(String name,
                        show_type type,
                        Company company,
                        String ownerId,
                        String managerId,
                        String venue,
                        String description) {

                Event event = eventRepository.findAll()
                                .stream()
                                .filter(existing -> existing.getName().equals(name))
                                .findFirst()
                                .orElseGet(() -> {
                                        Event created = new Event(name, type, company.getCompanyId(), ownerId);

                                        created.editVenue(venue, ownerId);
                                        created.editDescription(description, ownerId);
                                        created.editDates(daysFromNow(7), daysFromNow(8), ownerId);

                                        Event saved = eventRepository.saveAndFlush(created);

                                        if (!company.getAssociatedEventIds().contains(saved.getEventId())) {
                                                company.addEventId(company.getCompanyFounderId(), saved.getEventId());
                                                companyRepository.saveAndFlush(company);
                                        }

                                        return saved;
                                });

                if (!event.getManagerIds().contains(managerId)) {
                        event.addManager(managerId, ownerId);
                        eventRepository.saveAndFlush(event);
                }

                return event;
        }

        // private void addShowsIfMissing(Event event) {
        // if (event.getShows() != null && !event.getShows().isEmpty()) {
        // return;
        // }

        // show opening = new show(
        // event.getEventId(),
        // event.getName() + " - Opening Show",
        // "Opening show for demo testing.",
        // "Demo Artist A",
        // daysFromNow(7));

        // SeatedArea seatedArea = (new SeatedArea("Seated Area", 5));
        // SeatedArea seatedArea2 = (new SeatedArea("Seated Area 2", 5));

        // opening.setAreas(List.of(
        // new StandingArea("VIP", 100),
        // new StandingArea("Regular", 200),
        // new StandingArea("Student", 150),
        // seatedArea,
        // seatedArea2

        // ));

        // List<Block> blockA = List.of(
        // new Block(6, "Block A1", 10, seatedArea),
        // new Block(7, "Block A2", 5, seatedArea2),
        // new Block(8, "Block A3", 10, seatedArea),
        // new Block(9, "Block A4", 5, seatedArea2),
        // new Block(10, "Block A5", 10, seatedArea));
        // List<Block> blockB = List.of(
        // new Block(1, "Block B1", 8, seatedArea),
        // new Block(2, "Block B2", 8, seatedArea2),
        // new Block(3, "Block B3", 3, seatedArea),
        // new Block(4, "Block B4", 8, seatedArea2),
        // new Block(5, "Block B5", 8, seatedArea));

        // seatedArea.setBlocks(blockA);
        // seatedArea2.setBlocks(blockB);

        // show evening = new show(
        // event.getEventId(),
        // event.getName() + " - Evening Show",
        // "Evening show for demo testing.",
        // "Demo Artist B",
        // daysFromNow(8));

        // SeatedArea eveningSeatedArea1 = new SeatedArea("Seated Area", 5);
        // SeatedArea eveningSeatedArea2 = new SeatedArea("Seated Area 2", 5);

        // evening.setAreas(List.of(
        // new StandingArea("VIP", 100),
        // new StandingArea("Regular", 200),
        // new StandingArea("Student", 150),
        // eveningSeatedArea1,
        // eveningSeatedArea2));

        // event.addShow(opening, EVENT_OWNER_ID);
        // event.addShow(evening, EVENT_OWNER_ID);

        // eventRepository.saveAndFlush(event);
        // }
        private void addShowsIfMissing(Event event) {
                if (event.getShows() != null && !event.getShows().isEmpty()) {
                        return;
                }

                /*
                 * IMPORTANT:
                 * Your Block / Row / Seat constructors currently require manual IDs,
                 * even though the entities use @GeneratedValue.
                 *
                 * To reduce duplicate-ID problems between different demo events,
                 * this seed creates mostly unique IDs per event.
                 *
                 * Best long-term fix:
                 * remove the id parameter from Block / Row / Seat constructors and let DB
                 * generate IDs.
                 */
                long baseId = Integer.toUnsignedLong(event.getEventId().hashCode()) * 1_000_000L;
                long[] nextId = { baseId + 1L };

                // ============================================================
                // Show 1: Opening show
                // ============================================================
                show opening = new show(
                                event.getEventId(),
                                event.getName() + " - Opening Show",
                                "Opening show for demo testing. Includes standing areas and seated areas.",
                                "Demo Artist A",
                                daysFromNow(7));

                opening.setAreas(List.of(
                                new StandingArea("VIP Standing", 300),
                                new StandingArea("Regular Standing", 600),
                                new StandingArea("Student Standing", 250),
                                new StandingArea("Accessible Standing", 80),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Gold Seated Area",
                                                List.of("Gold A", "Gold B", "Gold C"),
                                                6,
                                                12),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Silver Seated Area",
                                                List.of("Silver A", "Silver B", "Silver C", "Silver D"),
                                                8,
                                                10),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Balcony Seated Area",
                                                List.of("Balcony A", "Balcony B"),
                                                5,
                                                14)));

                // ============================================================
                // Show 2: Main show
                // ============================================================
                show mainShow = new show(
                                event.getEventId(),
                                event.getName() + " - Main Show",
                                "Main headline show with the largest inventory.",
                                "Demo Artist B",
                                daysFromNow(8));

                mainShow.setAreas(List.of(
                                new StandingArea("Front Stage Standing", 500),
                                new StandingArea("General Standing", 1000),
                                new StandingArea("Student Zone", 400),
                                new StandingArea("Family Zone", 350),
                                new StandingArea("Accessible Zone", 120),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Premium Seats",
                                                List.of("Premium A", "Premium B", "Premium C", "Premium D"),
                                                10,
                                                15),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Regular Seats",
                                                List.of("Regular A", "Regular B", "Regular C", "Regular D",
                                                                "Regular E"),
                                                12,
                                                14),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Upper Seats",
                                                List.of("Upper A", "Upper B", "Upper C"),
                                                9,
                                                16)));

                // ============================================================
                // Show 3: Acoustic / smaller show
                // ============================================================
                show acousticShow = new show(
                                event.getEventId(),
                                event.getName() + " - Acoustic Session",
                                "Smaller acoustic session with limited seating.",
                                "Demo Artist C",
                                daysFromNow(9));

                acousticShow.setAreas(List.of(
                                new StandingArea("Small Standing Area", 150),
                                new StandingArea("Student Small Area", 80),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Acoustic Front Seats",
                                                List.of("Acoustic A", "Acoustic B"),
                                                4,
                                                10),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Acoustic Back Seats",
                                                List.of("Acoustic C", "Acoustic D"),
                                                5,
                                                12)));

                // ============================================================
                // Show 4: Closing show
                // ============================================================
                show closingShow = new show(
                                event.getEventId(),
                                event.getName() + " - Closing Show",
                                "Final closing show for the event.",
                                "Demo Artist D",
                                daysFromNow(10));

                closingShow.setAreas(List.of(
                                new StandingArea("Closing VIP Standing", 200),
                                new StandingArea("Closing Regular Standing", 500),
                                new StandingArea("Closing Student Standing", 200),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Closing Main Seats",
                                                List.of("Closing A", "Closing B", "Closing C"),
                                                7,
                                                12),

                                createSeatedAreaWithBlocks(
                                                nextId,
                                                "Closing Balcony Seats",
                                                List.of("Closing Balcony A", "Closing Balcony B"),
                                                5,
                                                15)));

                // event.addShow(opening, EVENT_OWNER_ID);
                // event.addShow(mainShow, EVENT_OWNER_ID);
                // event.addShow(acousticShow, EVENT_OWNER_ID);
                // event.addShow(closingShow, EVENT_OWNER_ID);

                eventRepository.saveAndFlush(event);
        }

        private SeatedArea createSeatedAreaWithBlocks(long[] nextId,
                        String areaName,
                        List<String> blockNames,
                        int rowsPerBlock,
                        int seatsPerRow) {

                SeatedArea seatedArea = new SeatedArea(areaName, blockNames.size());

                List<Block> blocks = new ArrayList<>();

                for (String blockName : blockNames) {
                        Block block = new Block(
                                        nextId[0]++,
                                        blockName,
                                        rowsPerBlock,
                                        seatedArea);

                        List<Row> rows = new ArrayList<>();

                        for (int rowIndex = 1; rowIndex <= rowsPerBlock; rowIndex++) {
                                Row row = new Row(
                                                nextId[0]++,
                                                "Row " + rowIndex,
                                                seatsPerRow,
                                                block);

                                List<Seat> seats = new ArrayList<>();

                                for (int seatIndex = 1; seatIndex <= seatsPerRow; seatIndex++) {
                                        Seat seat = new Seat(
                                                        nextId[0]++,
                                                        String.valueOf(seatIndex),
                                                        row);

                                        seats.add(seat);
                                }

                                row.setSeats(seats);
                                rows.add(row);
                        }

                        block.setRows(rows);
                        blocks.add(block);
                }

                seatedArea.setBlocks(blocks);

                return seatedArea;
        }

        private void createQueuesIfMissing() {
                createQueueIfMissing(9001L, 50, List.of(10001L, 10002L, 10003L, 10004L));
                createQueueIfMissing(9002L, 30, List.of(10005L, 10006L, 10007L));
                createQueueIfMissing(9003L, 10, List.of(10008L));
        }

        private void createQueueIfMissing(Long queueId, int capacityPerMinute, List<Long> userIds) {
                if (waitingQueueRepository.existsById(queueId)) {
                        return;
                }

                WaitingQueue queue = new WaitingQueue(queueId, capacityPerMinute);

                for (Long userId : userIds) {
                        queue.joinQueue(userId);
                }

                waitingQueueRepository.save(queue);
        }

        private void createPurchasesIfMissing(Member buyer,
                        Member buyer2,
                        Event event1,
                        Event event2) {

                if (purchaseRepository.findByBuyerId(buyer.getMemberId()).isEmpty()) {
                        Purchase purchase1 = createPurchaseFromDemoOrder(
                                        buyer.getMemberId(),
                                        event1.getEventId(),
                                        "VIP");

                        purchaseRepository.save(purchase1);
                }

                if (purchaseRepository.findByBuyerId(buyer2.getMemberId()).isEmpty()) {
                        Purchase purchase2 = createPurchaseFromDemoOrder(
                                        buyer2.getMemberId(),
                                        event2.getEventId(),
                                        "Regular");

                        purchaseRepository.save(purchase2);
                }
        }

        private Purchase createPurchaseFromDemoOrder(String buyerId, UUID eventId, String areaName) {
                UUID areaId = UUID.randomUUID();

                ActiveOrder order = new ActiveOrder(buyerId, eventId, 15);

                order.addTicket(
                                UUID.randomUUID().toString(),
                                1L,
                                areaId,
                                BigDecimal.valueOf(120.00),
                                new Lock(areaName + "-DEMO-TICKET-001", buyerId, LocalDateTime.now().plusMinutes(15)));

                order.addTicket(
                                UUID.randomUUID().toString(),
                                2L,
                                areaId,
                                BigDecimal.valueOf(180.00),
                                new Lock(areaName + "-DEMO-TICKET-002", buyerId, LocalDateTime.now().plusMinutes(15)));

                order.markCompleted();

                return new Purchase(order);
        }

        private void createActiveOrdersIfMissing(Member buyer, Event event) {
                if (!activeOrderRepository.findPendingOrdersByBuyer(buyer.getMemberId()).isEmpty()) {
                        return;
                }

                UUID areaId = UUID.randomUUID();

                ActiveOrder activeOrder = new ActiveOrder(
                                buyer.getMemberId(),
                                event.getEventId(),
                                20);

                activeOrder.addTicket(
                                UUID.randomUUID().toString(),
                                10L,
                                areaId,
                                BigDecimal.valueOf(99.90),
                                new Lock(UUID.randomUUID().toString(), buyer.getMemberId(),
                                                LocalDateTime.now().plusMinutes(20)));

                activeOrderRepository.save(activeOrder);
        }

        private void createComplaintsIfMissing(Member buyer,
                        Member buyer2,
                        Member user,
                        Member suspended) {

                if (complaintRepository.findByReporterMemberId(buyer.getMemberId()).isEmpty()) {
                        Complaint openComplaint = new Complaint(
                                        buyer.getMemberId(),
                                        "Problem with purchased ticket",
                                        "I bought a ticket but I want the system admin to check it.",
                                        "PURCHASE",
                                        "Demo purchase");

                        complaintRepository.save(openComplaint);
                }

                if (complaintRepository.findByReporterMemberId(buyer2.getMemberId()).isEmpty()) {
                        Complaint inProgressComplaint = new Complaint(
                                        buyer2.getMemberId(),
                                        "Event information looks wrong",
                                        "The event details look inconsistent.",
                                        "EVENT",
                                        "Demo Tech Conference");

                        inProgressComplaint.markInProgress();
                        complaintRepository.save(inProgressComplaint);
                }

                if (complaintRepository.findByReporterMemberId(user.getMemberId()).isEmpty()) {
                        Complaint resolvedComplaint = new Complaint(
                                        user.getMemberId(),
                                        "Question about account",
                                        "I had a question and admin already handled it.",
                                        "USER",
                                        user.getMemberId());

                        resolvedComplaint.resolve("Checked by demo admin. No further action needed.");
                        complaintRepository.save(resolvedComplaint);
                }

                if (complaintRepository.findByReporterMemberId(suspended.getMemberId()).isEmpty()) {
                        Complaint rejectedComplaint = new Complaint(
                                        suspended.getMemberId(),
                                        "Suspension complaint",
                                        "I want to appeal my suspension.",
                                        "USER",
                                        suspended.getMemberId());

                        rejectedComplaint.reject("Rejected in demo data.");
                        complaintRepository.save(rejectedComplaint);
                }
        }

        private void createNotificationsIfMissing(Member admin,
                        Member owner,
                        Member coOwner,
                        Member manager,
                        Member limitedManager,
                        Member buyer,
                        Member buyer2,
                        Member user,
                        Member suspended) {

                createNotificationsForUserIfMissing(admin.getMemberId(), List.of(
                                message("System dashboard demo data is ready.", NotificationType.SYSTEM_ANNOUNCEMENT),
                                message("There are demo complaints waiting for review.", NotificationType.GENERIC)));

                createNotificationsForUserIfMissing(owner.getMemberId(), List.of(
                                message("You are owner of the demo companies.", NotificationType.OWNER_APPOINTED),
                                message("Demo manager was assigned to your company.",
                                                NotificationType.MANAGER_APPOINTED),
                                message("Demo company has new events.", NotificationType.PRODUCER_MESSAGE)));

                createNotificationsForUserIfMissing(coOwner.getMemberId(), List.of(
                                message("You were appointed as co-owner.", NotificationType.OWNER_APPOINTED),
                                message("You can manage demo company roles.", NotificationType.ROLE_CHANGED)));

                createNotificationsForUserIfMissing(manager.getMemberId(), List.of(
                                message("You were appointed as manager with full permissions.",
                                                NotificationType.MANAGER_APPOINTED),
                                message("Your permissions were updated.", NotificationType.PERMISSIONS_CHANGED)));

                createNotificationsForUserIfMissing(limitedManager.getMemberId(), List.of(
                                message("You were appointed as limited manager.", NotificationType.MANAGER_APPOINTED)));

                createNotificationsForUserIfMissing(buyer.getMemberId(), List.of(
                                message("Your demo purchase was completed successfully.",
                                                NotificationType.PURCHASE_SUCCESS),
                                message("Welcome buyer_demo. Notification bell is ready.", NotificationType.GENERIC)));

                createNotificationsForUserIfMissing(buyer2.getMemberId(), List.of(
                                message("You have an active demo order.", NotificationType.ORDER_EXPIRY_WARNING),
                                message("Your second demo purchase was completed.",
                                                NotificationType.PURCHASE_SUCCESS)));

                createNotificationsForUserIfMissing(user.getMemberId(), List.of(
                                message("Welcome user_demo.", NotificationType.GENERIC),
                                message("You can submit complaints and browse events.",
                                                NotificationType.SYSTEM_ANNOUNCEMENT)));

                createNotificationsForUserIfMissing(suspended.getMemberId(), List.of(
                                message("Your account is suspended in demo data.",
                                                NotificationType.SYSTEM_ANNOUNCEMENT)));
        }

        private DemoNotification message(String text, NotificationType type) {
                return new DemoNotification(text, type);
        }

        private void createNotificationsForUserIfMissing(String memberId, List<DemoNotification> notifications) {
                if (!notificationService.getNotificationsForUser(memberId).isEmpty()) {
                        return;
                }

                for (DemoNotification notification : notifications) {
                        notificationService.createNotification(
                                        memberId,
                                        notification.message(),
                                        notification.type());
                }
        }

        private Date daysFromNow(int days) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, days);
                return calendar.getTime();
        }

        private void printCredentials() {
                System.out.println(
                                "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\");
                System.out.println("Demo users, password for all = " + PASSWORD);
                System.out.println("admin_demo");
                System.out.println("owner_demo");
                System.out.println("co_owner_demo");
                System.out.println("manager_demo");
                System.out.println("limited_manager_demo");
                System.out.println("buyer_demo");
                System.out.println("buyer2_demo");
                System.out.println("user_demo");
                System.out.println("suspended_demo");
                System.out.println(
                                "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\");
                System.out.println("Demo queue IDs:");
                System.out.println("9001, 9002, 9003");
                System.out.println(
                                "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\");
        }

        private record DemoNotification(String message, NotificationType type) {
        }
}