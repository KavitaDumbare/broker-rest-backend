package com.radianbroker.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.radianbroker.entity.Role;
import com.radianbroker.entity.User.TwoFactorAuthType;

public class UserProfile {

	private Long userId;

	private String firstName;

	private String lastName;

	private String userName;

	private String email;

	private boolean emailHide;

	private String passwordHash;

	private String phoneNumber;

	private boolean phoneNumberHide;

	private LocalDateTime registrationDate;

	private LocalDateTime lastLoginDate;

	private boolean status;

	private LocalDateTime statusChangeDate;

	private Long groupIdForAdmin;

	private String companyName;

	private boolean companyNameHide;

	private String abn;

	private String accountName;

	private String bsb;

	private String accountNumber;

	private String photoFilePath;

	private String photoUrl;

	private String biography;

	private TwoFactorAuthType twoFactorAuthType;

	private List<Role> roles;

	private String sessionRole;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmailHide() {
		return emailHide;
	}

	public void setEmailHide(boolean emailHide) {
		this.emailHide = emailHide;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public boolean isPhoneNumberHide() {
		return phoneNumberHide;
	}

	public void setPhoneNumberHide(boolean phoneNumberHide) {
		this.phoneNumberHide = phoneNumberHide;
	}

	public LocalDateTime getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(LocalDateTime registrationDate) {
		this.registrationDate = registrationDate;
	}

	public LocalDateTime getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(LocalDateTime lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public LocalDateTime getStatusChangeDate() {
		return statusChangeDate;
	}

	public void setStatusChangeDate(LocalDateTime statusChangeDate) {
		this.statusChangeDate = statusChangeDate;
	}

	public Long getGroupIdForAdmin() {
		return groupIdForAdmin;
	}

	public void setGroupIdForAdmin(Long groupIdForAdmin) {
		this.groupIdForAdmin = groupIdForAdmin;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public boolean isCompanyNameHide() {
		return companyNameHide;
	}

	public void setCompanyNameHide(boolean companyNameHide) {
		this.companyNameHide = companyNameHide;
	}

	public String getAbn() {
		return abn;
	}

	public void setAbn(String abn) {
		this.abn = abn;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getBsb() {
		return bsb;
	}

	public void setBsb(String bsb) {
		this.bsb = bsb;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getPhotoFilePath() {
		return photoFilePath;
	}

	public void setPhotoFilePath(String photoFilePath) {
		this.photoFilePath = photoFilePath;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public String getBiography() {
		return biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public TwoFactorAuthType getTwoFactorAuthType() {
		return twoFactorAuthType;
	}

	public void setTwoFactorAuthType(TwoFactorAuthType twoFactorAuthType) {
		this.twoFactorAuthType = twoFactorAuthType;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	public String getSessionRole() {
		return sessionRole;
	}

	public void setSessionRole(String sessionRole) {
		this.sessionRole = sessionRole;
	}

}
