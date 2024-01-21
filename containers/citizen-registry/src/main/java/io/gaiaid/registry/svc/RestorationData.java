package io.gaiaid.registry.svc;

import java.time.LocalDate;

import io.gaiaid.registry.enums.RestorationState;

public class RestorationData {
	private RestorationState state;
	private String firstname;
	private String lastname;
	private LocalDate birthdate;

	public RestorationState getState() {
		return state;
	}
	public void setState(RestorationState state) {
		this.state = state;
	}
	public String getFirstname() {
		return firstname;
	}
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	public String getLastname() {
		return lastname;
	}
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	public LocalDate getBirthdate() {
		return birthdate;
	}
	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}
	
}
