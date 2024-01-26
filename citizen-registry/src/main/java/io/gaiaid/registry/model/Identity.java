package io.gaiaid.registry.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import io.gaiaid.registry.enums.IdentityClaim;
import io.gaiaid.registry.enums.Protection;


/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="identity")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Identity.findForConnection", query="SELECT i FROM Identity i where i.connectionId=:connectionId and (i.deletedTs IS NULL or i.deletedTs>:deletedTs)  ORDER by i.id ASC"),
	@NamedQuery(name="Identity.findForRestorePassword", query="SELECT i FROM Identity i where i.connectionId<>:connectionId and (i.deletedTs IS NULL or i.deletedTs>:deletedTs) and i.firstname=:firstname and i.lastname=:lastname and i.birthdate=:birthdate and i.password=:password and i.protection=:protection  ORDER by i.id ASC"),
	@NamedQuery(name="Identity.findForRestoreOthers", query="SELECT i FROM Identity i where i.connectionId<>:connectionId and (i.deletedTs IS NULL or i.deletedTs>:deletedTs) and i.firstname=:firstname and i.lastname=:lastname and i.birthdate=:birthdate and i.protection=:protection  ORDER by i.id ASC"),
	// existing: same claims, and not deleted or deleted for less than recoverable period
	@NamedQuery(name="Identity.findExisting", query="SELECT i FROM Identity i where i.firstname=:firstname and i.lastname=:lastname and i.birthdate=:birthdate and i.placeOfBirth=:placeOfBirth and (i.deletedTs IS NULL or i.deletedTs>:deletedTs)"),
	
})
public class Identity implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	private UUID id;
	
	private UUID connectionId;
	
	@Column(columnDefinition="timestamptz")
	private Instant startedTs;
	@Column(columnDefinition="timestamptz")
	private Instant completedTs;
	@Column(columnDefinition="timestamptz")
	private Instant confirmedTs;
	@Column(columnDefinition="timestamptz")
	private Instant protectedTs;
	@Column(columnDefinition="timestamptz")
	private Instant issuedTs;
	@Column(columnDefinition="timestamptz")
	private Instant revokedTs;
	@Column(columnDefinition="timestamptz")
	private Instant deletedTs;
	
	@Column(columnDefinition="timestamptz")
	private Instant authenticatedTs;
	
	
	
	private IdentityClaim creationStep;
	private IdentityClaim changeStep;
	
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
	@Column(columnDefinition="timestamptz")
	private Instant citizenSinceTs;
	
	private Protection protection;
	@Column(columnDefinition="text")
	private String password;

	

	public UUID getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}

	public Instant getCompletedTs() {
		return completedTs;
	}

	public void setCompletedTs(Instant completedTs) {
		this.completedTs = completedTs;
	}

	public Instant getIssuedTs() {
		return issuedTs;
	}

	public void setIssuedTs(Instant issuedTs) {
		this.issuedTs = issuedTs;
	}

	public Instant getRevokedTs() {
		return revokedTs;
	}

	public void setRevokedTs(Instant revokedTs) {
		this.revokedTs = revokedTs;
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


	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	

	public Instant getCitizenSinceTs() {
		return citizenSinceTs;
	}

	public void setCitizenSinceTs(Instant citizenSinceTs) {
		this.citizenSinceTs = citizenSinceTs;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public IdentityClaim getCreationStep() {
		return creationStep;
	}

	public void setCreationStep(IdentityClaim creationStep) {
		this.creationStep = creationStep;
	}

	

	public IdentityClaim getChangeStep() {
		return changeStep;
	}

	public void setChangeStep(IdentityClaim changeStep) {
		this.changeStep = changeStep;
	}

	public Instant getConfirmedTs() {
		return confirmedTs;
	}

	public void setConfirmedTs(Instant confirmedTs) {
		this.confirmedTs = confirmedTs;
	}

	public Instant getStartedTs() {
		return startedTs;
	}

	public void setStartedTs(Instant startedTs) {
		this.startedTs = startedTs;
	}

	public Instant getProtectedTs() {
		return protectedTs;
	}

	public void setProtectedTs(Instant protectedTs) {
		this.protectedTs = protectedTs;
	}

	public Protection getProtection() {
		return protection;
	}

	public void setProtection(Protection protection) {
		this.protection = protection;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Instant getDeletedTs() {
		return deletedTs;
	}

	public void setDeletedTs(Instant deletedTs) {
		this.deletedTs = deletedTs;
	}

	public LocalDate getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	public Instant getAuthenticatedTs() {
		return authenticatedTs;
	}

	public void setAuthenticatedTs(Instant authenticatedTs) {
		this.authenticatedTs = authenticatedTs;
	}

	public String getCitizenId() {
		return citizenId;
	}

	public void setCitizenId(String citizenId) {
		this.citizenId = citizenId;
	}

	public String getAvatarname() {
		return avatarname;
	}

	public void setAvatarname(String avatarname) {
		this.avatarname = avatarname;
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
	
	
}