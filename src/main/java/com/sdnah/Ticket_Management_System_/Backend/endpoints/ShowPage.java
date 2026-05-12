package com.sdnah.Ticket_Management_System_.Backend.endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/ShowPage")
public class ShowPage {
    @GetMapping("/ShowDetailsByShowId")
    public ResponseEntity<String> ShowDetailsByShowId(Long showId) {
        return ResponseEntity.ok("show details by show ID endpoint!");
    }
    @GetMapping("/ShowAvailableSeatsByShowId")
    public ResponseEntity<String> ShowAvailableSeatsByShowId(Long showId) {
        return ResponseEntity.ok("show available seats by show ID endpoint!");
    }
    //click button to reserve seat
    @GetMapping("/ReserveSeatButton")
    public ResponseEntity<String> ReserveSeatButton(Long showId, Long seatId) {
        return ResponseEntity.ok("reserve seat button endpoint!");
    }
    //buttong to go to AreaSelectionPage
    @GetMapping("/GoToAreaSelectionPageButton")
    public ResponseEntity<String> GoToAreaSelectionPageButton(Long showId) {
        return ResponseEntity.ok("go to area selection page button endpoint!");
    }
}
