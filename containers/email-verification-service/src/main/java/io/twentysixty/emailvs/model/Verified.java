package io.twentysixty.emailvs.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import io.twentysixty.emailvs.enums.EmailType;
import io.twentysixty.emailvs.enums.VerificationStep;


/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="verified")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Verified.findForConnection", query="SELECT i FROM Verified i where i.connectionId=:connectionId and (i.deletedTs IS NULL or i.deletedTs>:deletedTs)  ORDER by i.id ASC"),
	@NamedQuery(name="Verified.purgeOld", query="UPDATE Verified i set i.revokedTs=:revokedTs, i.verifiedTs=:verifiedTs, i.deletedTs=:deletedTs where i.connectionId<>:connectionId and i.email=:email"),
	
})

public class Verified implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	//@Column(columnDefinition="text")
	private UUID id;
	private UUID connectionId;

	private String email;
	
	
	 
	private String otp;
	@Column(columnDefinition="timestamptz")
	private Instant startedTs;
	@Column(columnDefinition="timestamptz")
	private Instant sentTs;
	@Column(columnDefinition="timestamptz")
	private Instant verifiedTs;
	@Column(columnDefinition="timestamptz")
	private Instant revokedTs;
	@Column(columnDefinition="timestamptz")
	private Instant issuedTs;
	@Column(columnDefinition="timestamptz")
	private Instant deletedTs;
	@Column(columnDefinition="timestamptz")
	private Instant confirmedTs;
	
	private VerificationStep verificationStep;
	
	private EmailType type;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public Instant getSentTs() {
		return sentTs;
	}
	public void setSentTs(Instant sentTs) {
		this.sentTs = sentTs;
	}
	
	public String getOtp() {
		return otp;
	}
	public void setOtp(String otp) {
		this.otp = otp;
	}
	public Instant getRevokedTs() {
		return revokedTs;
	}
	public void setRevokedTs(Instant revokedTs) {
		this.revokedTs = revokedTs;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public UUID getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}
	public Instant getIssuedTs() {
		return issuedTs;
	}
	public void setIssuedTs(Instant issuedTs) {
		this.issuedTs = issuedTs;
	}
	public Instant getDeletedTs() {
		return deletedTs;
	}
	public void setDeletedTs(Instant deletedTs) {
		this.deletedTs = deletedTs;
	}
	public Instant getConfirmedTs() {
		return confirmedTs;
	}
	public void setConfirmedTs(Instant confirmedTs) {
		this.confirmedTs = confirmedTs;
	}
	public VerificationStep getVerificationStep() {
		return verificationStep;
	}
	public void setVerificationStep(VerificationStep verificationStep) {
		this.verificationStep = verificationStep;
	}
	public EmailType getType() {
		return type;
	}
	public void setType(EmailType type) {
		this.type = type;
	}
	public Instant getStartedTs() {
		return startedTs;
	}
	public void setStartedTs(Instant startedTs) {
		this.startedTs = startedTs;
	}
	public Instant getVerifiedTs() {
		return verifiedTs;
	}
	public void setVerifiedTs(Instant verifiedTs) {
		this.verifiedTs = verifiedTs;
	}
	
	
	
	
}