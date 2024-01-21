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

import io.gaiaid.registry.enums.MediaType;


/**
 * The persistent class for the session database table.
 * 
 */
@Entity
@Table(name="media")
@DynamicUpdate
@DynamicInsert
@NamedQueries({
	@NamedQuery(name="Media.find", query="SELECT m.id FROM Media m where m.identity=:identity and m.type=:type ORDER by m.ts ASC"),
	@NamedQuery(name="Media.delete", query="DELETE FROM Media m where m.identity=:identity and m.type=:type"),
	
})
public class Media implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	private UUID id;
	
	@ManyToOne
	@JoinColumn(name="identity_fk")
	private Identity identity;
	
	
	private MediaType type;
	
	
	@Column(columnDefinition="text")
	private String mimeType;
	
	
	@Column(columnDefinition="timestamptz")
	private Instant ts;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Identity getIdentity() {
		return identity;
	}

	public void setIdentity(Identity identity) {
		this.identity = identity;
	}

	public Instant getTs() {
		return ts;
	}

	public void setTs(Instant ts) {
		this.ts = ts;
	}

	public MediaType getType() {
		return type;
	}

	public void setType(MediaType type) {
		this.type = type;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	

	
	
	
}