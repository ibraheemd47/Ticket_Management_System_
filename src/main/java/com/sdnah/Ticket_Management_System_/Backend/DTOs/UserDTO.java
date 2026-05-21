package com.sdnah.Ticket_Management_System_.Backend.DTOs;

public class UserDTO {
	private String memberId;
	private String username;
	private String password;
	private String token;
	private String email;
	private String code;
	private String newPassword;
	private boolean isActive;
	private boolean isSuspended;
	private boolean isSuspendedPermanently;

	public UserDTO() {
	}


	public UserDTO(String memberId, String username, String password, String token, String email, String code, String newPassword, boolean isActive, boolean isSuspended, boolean isSuspendedPermanently) {
		this.memberId = memberId;
		this.username = username;
		this.password = password;
		this.token = token;
		this.email = email;
		this.code = code;
		this.newPassword = newPassword;
		this.isActive = isActive;
		this.isSuspended = isSuspended;
		this.isSuspendedPermanently = isSuspendedPermanently;
	}

	public void setActive(boolean active) {
		isActive = active;
	}
	public boolean isActive() {
		return isActive;
	}
	public boolean isSuspended() {
		return isSuspended;
	}
	public void setSuspended(boolean suspended) {
		isSuspended = suspended;
	}
	public boolean isSuspendedPermanently() {
		return isSuspendedPermanently;
	}
	public void setSuspendedPermanently(boolean suspendedPermanently) {
		isSuspendedPermanently = suspendedPermanently;
	}
	public String getMemberId() {
		return memberId;
	}
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

}
