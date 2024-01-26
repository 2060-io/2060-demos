package io.gaiaid.registry.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import io.gaiaid.registry.enums.CreateStep;
import io.gaiaid.registry.enums.IssueStep;
import io.gaiaid.registry.enums.RestoreStep;
import io.gaiaid.registry.enums.SessionType;


/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="session")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	
})
public class Session implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	private UUID connectionId;
	
	@ManyToOne
	@JoinColumn(name="identity_fk")
	private Identity identity;
	
	
	private SessionType type;
	
	private CreateStep createStep;
	private RestoreStep restoreStep;
	private IssueStep issueStep;
	
	
	@Column(columnDefinition="text")
	private String citizenId;
	@Column(columnDefinition="text")
	private String firstname;
	@Column(columnDefinition="text")
	private String lastname;
	@Column(columnDefinition="text")
	private String avatarname;
	private UUID avatarPic;
	@Column(columnDefinition="text")
	private String avatarURI;
	
	
	@Column(columnDefinition="text")
	private String avatarMimeType;
	
	@Column(columnDefinition="text")
	private String avatarPicCiphKey;
	@Column(columnDefinition="text")
	private String avatarPicCiphIv;
	
	@Column(columnDefinition="text")
	private String avatarPicCiphAlg;
	
	
	@Column(columnDefinition="date")
	private LocalDate birthdate;
	@Column(columnDefinition="text")
	private String placeOfBirth;
	
	
	public UUID getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}
	public CreateStep getCreateStep() {
		return createStep;
	}
	public void setCreateStep(CreateStep createStep) {
		this.createStep = createStep;
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
	public String getPlaceOfBirth() {
		return placeOfBirth;
	}
	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}
	public RestoreStep getRestoreStep() {
		return restoreStep;
	}
	public void setRestoreStep(RestoreStep restoreStep) {
		this.restoreStep = restoreStep;
	}
	public Identity getIdentity() {
		return identity;
	}
	public void setIdentity(Identity identity) {
		this.identity = identity;
	}
	public SessionType getType() {
		return type;
	}
	public void setType(SessionType type) {
		this.type = type;
	}
	public IssueStep getIssueStep() {
		return issueStep;
	}
	public void setIssueStep(IssueStep issueStep) {
		this.issueStep = issueStep;
	}
	
	public String getAvatarname() {
		return avatarname;
	}
	public void setAvatarname(String avatarname) {
		this.avatarname = avatarname;
	}
	public String getCitizenId() {
		return citizenId;
	}
	public void setCitizenId(String citizenId) {
		this.citizenId = citizenId;
	}
	public UUID getAvatarPic() {
		return avatarPic;
	}
	public void setAvatarPic(UUID avatarPic) {
		this.avatarPic = avatarPic;
	}
	public String getAvatarMimeType() {
		return avatarMimeType;
	}
	public void setAvatarMimeType(String avatarMimeType) {
		this.avatarMimeType = avatarMimeType;
	}
	public String getAvatarPicCiphKey() {
		return avatarPicCiphKey;
	}
	public void setAvatarPicCiphKey(String avatarPicCiphKey) {
		this.avatarPicCiphKey = avatarPicCiphKey;
	}
	public String getAvatarPicCiphIv() {
		return avatarPicCiphIv;
	}
	public void setAvatarPicCiphIv(String avatarPicCiphIv) {
		this.avatarPicCiphIv = avatarPicCiphIv;
	}
	public String getAvatarPicCiphAlg() {
		return avatarPicCiphAlg;
	}
	public void setAvatarPicCiphAlg(String avatarPicCiphAlg) {
		this.avatarPicCiphAlg = avatarPicCiphAlg;
	}
	public String getAvatarURI() {
		return avatarURI;
	}
	public void setAvatarURI(String avatarURI) {
		this.avatarURI = avatarURI;
	}
	
	
	

	
}