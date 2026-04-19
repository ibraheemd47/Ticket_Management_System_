package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;

class SystemAdminTest {

    @Test
    void constructor_withExplicitAssigner_setsAdminRoleAndAssigner() {
        Member member = new Member("m1", "mostafa", "hashed123");

        System_admin admin = new System_admin(member, "super-admin");

        assertEquals("m1", admin.getMemberId());
        assertEquals("mostafa", admin.getUsername());
        assertEquals("hashed123", admin.getPasswordHash());
        assertEquals(UserRole.SYSTEM_ADMIN, admin.getRole());
        assertEquals("super-admin", admin.getWhoAssigned());
    }

    @Test
    void constructor_withDefaultAssigner_setsSystemAsAssigner() {
        Member member = new Member("m1", "mostafa", "hashed123");

        System_admin admin = new System_admin(member);

        assertEquals("System", admin.getWhoAssigned());
    }
}
