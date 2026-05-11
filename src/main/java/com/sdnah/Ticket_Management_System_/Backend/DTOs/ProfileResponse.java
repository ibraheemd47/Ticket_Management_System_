package com.sdnah.Ticket_Management_System_.Backend.DTOs;


import java.time.LocalDate;

public class ProfileResponse {
    private String memberId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private LocalDate birthDate;
    private String role;
    private boolean active;
    private boolean loggedin;
    private boolean verified;

    public ProfileResponse() {
    }

    public ProfileResponse(String memberId, String username, String firstName, String lastName,
                           String email, String phone, String address, String city,
                           String country, LocalDate birthDate, String role,
                           boolean active, boolean loggedin, boolean verified) {
        this.memberId = memberId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.country = country;
        this.birthDate = birthDate;
        this.role = role;
        this.active = active;
        this.loggedin = loggedin;
        this.verified = verified;
    }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isLoggedin() { return loggedin; }
    public void setLoggedin(boolean loggedin) { this.loggedin = loggedin; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}