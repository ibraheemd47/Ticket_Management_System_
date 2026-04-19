package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
class CompanyRoleServiceAcceptanceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CompanyRoleService companyRoleService;

    private Member actorOwner;
    private Member targetMember;

    @BeforeEach
    void setUp() {
        actorOwner = new Member("owner1", "ownerUser", "hash");
        actorOwner.addCompanyRole(new CompanyRoleAssignment(
                "company1", "owner1", CompanyRoleType.OWNER, Set.of()));

        targetMember = new Member("member2", "targetUser", "hash");
    }

    @Test
    void assignOwner_byExistingOwner_addsOwnerRoleToTarget() {
        when(userService.getMemberByToken("valid-token")).thenReturn(actorOwner);
        when(userRepository.findById("member2")).thenReturn(Optional.of(targetMember));

        companyRoleService.assignOwner("valid-token", "company1", "member2");

        assertTrue(targetMember.isOwnerInCompany("company1"));
        verify(userRepository).save(targetMember);
    }

    @Test
    void assignOwner_byNonOwner_throwsException() {
        Member nonOwner = new Member("member1", "regular", "hash");
        when(userService.getMemberByToken("valid-token")).thenReturn(nonOwner);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner("valid-token", "company1", "member2"));

        assertEquals("Only Company owner can assign ownership", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignOwner_whenTargetAlreadyHasRole_throwsException() {
        targetMember.addCompanyRole(new CompanyRoleAssignment(
                "company1", "owner1", CompanyRoleType.MANAGER, Set.of()));

        when(userService.getMemberByToken("valid-token")).thenReturn(actorOwner);
        when(userRepository.findById("member2")).thenReturn(Optional.of(targetMember));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner("valid-token", "company1", "member2"));

        assertEquals("Target already has a role in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignManager_byExistingOwner_addsManagerRoleToTarget() {
        when(userService.getMemberByToken("valid-token")).thenReturn(actorOwner);
        when(userRepository.findById("member2")).thenReturn(Optional.of(targetMember));

        companyRoleService.assignManager("valid-token", "company1", "member2");

        assertTrue(targetMember.isManagerInCompany("company1"));
        verify(userRepository).save(targetMember);
    }

    @Test
    void assignManager_byNonOwner_throwsException() {
        Member nonOwner = new Member("member1", "regular", "hash");
        when(userService.getMemberByToken("valid-token")).thenReturn(nonOwner);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignManager("valid-token", "company1", "member2"));

        assertEquals("Only Company owner can assign manager", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignManager_whenTargetNotFound_throwsException() {
        when(userService.getMemberByToken("valid-token")).thenReturn(actorOwner);
        when(userRepository.findById("member2")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignManager("valid-token", "company1", "member2"));

        assertEquals("Target member not found", ex.getMessage());
    }
}
