package io.gaiaid.registry.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import io.gaiaid.registry.enums.TokenType;


/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="token")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Token.findForConnection", query="SELECT t FROM Token t where t.connectionId=:connectionId and t.type=:type "),
	
})
public class Token implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	private UUID id;
	
	@Column(columnDefinition="timestamptz")
	private Instant expireTs;

	
	@ManyToOne
	@JoinColumn(name="identity_fk")
	private Identity identity;
	
	
	
	
	private UUID connectionId;
	private UUID threadId;
	
	private TokenType type;
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Instant getExpireTs() {
		return expireTs;
	}

	public void setExpireTs(Instant expireTs) {
		this.expireTs = expireTs;
	}

	public Identity getIdentity() {
		return identity;
	}

	public void setIdentity(Identity identity) {
		this.identity = identity;
	}

	public UUID getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}

	public UUID getThreadId() {
		return threadId;
	}

	public void setThreadId(UUID threadId) {
		this.threadId = threadId;
	}

	

	public TokenType getType() {
		return type;
	}

	public void setType(TokenType type) {
		this.type = type;
	}

	
	
}