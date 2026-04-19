package com.sdnah.Ticket_Management_System_.endpoints;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stam")
public class stam {

    // Simple GET endpoint: /api/stam/hello
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from stam endpoint!");
    }

    // Simple POST endpoint: /api/stam/echo
    @PostMapping("/echo")
    public ResponseEntity<EchoResponse> echo(@RequestBody EchoRequest request) {
        EchoResponse resp = new EchoResponse("Received", request.getMessage());
        return ResponseEntity.ok(resp);
    }

    // DTOs as static inner classes for simplicity
    public static class EchoRequest {
        private String message;
        public EchoRequest() {}
        public EchoRequest(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class EchoResponse {
        private String status;
        private String message;
        public EchoResponse() {}
        public EchoResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
