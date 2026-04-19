package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;

public class MemberTest {
    @Test
    void constructor_initializesMemberCorrectly() {
        Member member = new Member("m1", "mostafa", "hashed123");

        assertEquals("m1", member.getMemberId());
        assertEquals("mostafa", member.getUsername());
        assertEquals("hashed123", member.getPasswordHash());
        assertTrue(member.isActive());
        assertTrue(member.isLoggedin());
        assertEquals(UserRole.MEMBER, member.getRole());
        assertTrue(member.getCompanyRoles().isEmpty());
    }

    @Test
    void logout_marksMemberAsLoggedOut() {
        Member member = new Member("m1", "mostafa", "hashed123");

        member.logout();

        assertFalse(member.isLoggedin());
    }

    @Test
    void login_marksMemberAsLoggedIn() {
        Member member = new Member("m1", "mostafa", "hashed123");
        member.logout();

        member.login();

        assertTrue(member.isLoggedin());
    }

    @Test
    void addCompanyRole_addsRoleToMember() {
        Member member = new Member("m1", "mostafa", "hashed123");
        CompanyRoleAssignment assignment = new CompanyRoleAssignment("c1", "owner1", CompanyRoleType.OWNER, Set.of());

        member.addCompanyRole(assignment);

        assertEquals(1, member.getCompanyRoles().size());
        assertTrue(member.getRoleInCompany("c1").isPresent());
    }

    @Test
    void isOwnerInCompany_returnsTrueWhenOwnerRoleExists() {
        Member member = new Member("m1", "mostafa", "hashed123");
        CompanyRoleAssignment assignment = new CompanyRoleAssignment("c1", "owner1", CompanyRoleType.OWNER, Set.of());

        member.addCompanyRole(assignment);

        assertTrue(member.isOwnerInCompany("c1"));
        assertFalse(member.isManagerInCompany("c1"));
    }

    @Test
    void isManagerInCompany_returnsTrueWhenManagerRoleExists() {
        Member member = new Member("m1", "mostafa", "hashed123");
        CompanyRoleAssignment assignment = new CompanyRoleAssignment("c1", "owner1", CompanyRoleType.MANAGER,
                Set.of(ManagerPermission.ADD_PRODUCT));

        member.addCompanyRole(assignment);

        assertTrue(member.isManagerInCompany("c1"));
        assertFalse(member.isOwnerInCompany("c1"));
    }

    @Test
    void getRoleInCompany_returnsEmptyWhenCompanyRoleDoesNotExist() {
        Member member = new Member("m1", "mostafa", "hashed123");

        assertTrue(member.getRoleInCompany("missing-company").isEmpty());
        assertFalse(member.isOwnerInCompany("missing-company"));
        assertFalse(member.isManagerInCompany("missing-company"));
    }

    @Test
    void getCompanyRoles_returnsUnmodifiableSet() {
        Member member = new Member("m1", "mostafa", "hashed123");
        CompanyRoleAssignment assignment = new CompanyRoleAssignment("c1", "owner1", CompanyRoleType.OWNER, Set.of());
        member.addCompanyRole(assignment);

        Set<CompanyRoleAssignment> rolesView = member.getCompanyRoles();

        assertThrows(UnsupportedOperationException.class, () -> rolesView.add(
                new CompanyRoleAssignment("c2", "owner2", CompanyRoleType.MANAGER, Set.of())));
    }

}
