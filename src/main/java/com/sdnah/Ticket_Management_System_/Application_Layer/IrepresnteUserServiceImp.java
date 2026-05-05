// package com.sdnah.Ticket_Management_System_.Application_Layer;

// import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
// public class IrepresnteUserServiceImp implements IrepresnteUserService {
//     private final UserService userService;

//     public IrepresnteUserServiceImp(UserService userService) {
//         this.userService = userService;
//     }

//     @Override
//     public Member requireMember(String token) {
//         return userService.getMemberByToken(token);
//     }

//     @Override
//     public String requireMemberId(String token) {
//         return userService.getMemberByToken(token).getMemberId();
//     }

//     @Override
//     public boolean validateToken(String token) {
//         return userService.validateToken(token);
//     }

// }
