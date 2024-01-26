package io.gaiaid.registry.svc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.gaiaid.registry.enums.MediaType;
import io.gaiaid.registry.enums.TokenType;
import io.gaiaid.registry.ex.MediaAlreadyLinkedException;
import io.gaiaid.registry.ex.TokenException;
import io.gaiaid.registry.model.Identity;
import io.gaiaid.registry.model.Media;
import io.gaiaid.registry.model.Token;

@ApplicationScoped
public class VisionService {

	
	@Inject GaiaService service;
	
	@Inject EntityManager em;
	
	
	@ConfigProperty(name = "io.gaiaid.create.token.lifetimeseconds")
	Long createTokenLifetimeSec;
	
	@ConfigProperty(name = "io.gaiaid.verify.token.lifetimeseconds")
	Long verifyTokenLifetimeSec;
	
	
	
	public List<UUID> listMedias(UUID tokenId) throws Exception {
		
		Token token = this.getToken(tokenId);
		if (token == null) throw new TokenException();
		
		Identity identity = token.getIdentity();
		
		
		if ((identity.getProtectedTs() != null) 
				&& (identity.getDeletedTs() == null) && (identity.getConnectionId() != null)) {
			
			Query q = this.em.createNamedQuery("Media.find");
			q.setParameter("identity", identity);
			
			
			switch (token.getType()) {
				case FACE_VERIFICATION: {
					
					q.setParameter("type", MediaType.FACE);
					List<UUID> medias = q.getResultList();
					
					return medias;
				}
				case FINGERPRINT_VERIFICATION: {
					q.setParameter("type", MediaType.FINGERPRINT);
					List<UUID> medias = q.getResultList();
					
					return medias;
				}
				default: {
					throw new TokenException();
				}
			}
			
			
		}
		
		throw new TokenException();
		
	}

	@Transactional
	public void linkMedia(UUID tokenId, UUID mediaId) throws Exception {
		
		
		Token token = this.getToken(tokenId);
		if (token == null) throw new TokenException();
		
		Identity identity = token.getIdentity();
		Media media = em.find(Media.class, mediaId);
		if (media != null) {
			throw new MediaAlreadyLinkedException();
		}
		
		if ((identity.getDeletedTs() == null) && (identity.getConnectionId() != null)) {
			switch (token.getType()) {
			
			case FACE_VERIFICATION:
			case FACE_CAPTURE: {
				
				media = new Media();
				media.setId(mediaId);
				media.setIdentity(identity);
				media.setTs(Instant.now());
				media.setType(MediaType.FACE);
				em.persist(media);
				break;
			}
			
			case FINGERPRINT_VERIFICATION:
			case FINGERPRINT_CAPTURE: {
				
				media = new Media();
				media.setId(mediaId);
				media.setIdentity(identity);
				media.setTs(Instant.now());
				media.setType(MediaType.FINGERPRINT);
				em.persist(media);
				break;
			}
			
			
			}
		} else {
			throw new TokenException();
		}
		
	}

	
	private Token getToken(UUID uuid) throws TokenException {
		Token t = em.find(Token.class, uuid);
		if (t == null) throw new TokenException();
		if (t.getExpireTs() == null) throw new TokenException();
		if (t.getExpireTs().isBefore(Instant.now())) throw new TokenException();
		return t;
	}

	public void success(UUID tokenId) throws Exception {
		
		Token token = this.getToken(tokenId);
		if (token == null) throw new TokenException();
		
		service.notifySuccess(token);
		
		
	}

	
	public void failure(UUID tokenId) throws Exception {
		Token token = this.getToken(tokenId);
		if (token == null) throw new TokenException();
		
		service.notifyFailure(token);
		
	
		
	}
	
	
	public Token createToken(TokenType tokenType, Identity identity, UUID connectionId, UUID threadId) {
		Token token = new Token();
		token.setId(UUID.randomUUID());
		token.setConnectionId(connectionId);
		token.setThreadId(threadId);
		token.setIdentity(identity);
		token.setType(tokenType);
		
		switch (tokenType) {
		case FACE_CAPTURE:
		case FINGERPRINT_CAPTURE:
		{
			token.setExpireTs(Instant.now().plus(Duration.ofSeconds(createTokenLifetimeSec)));
			break;
		}
		case FACE_VERIFICATION:
		case FINGERPRINT_VERIFICATION: {
			token.setExpireTs(Instant.now().plus(Duration.ofSeconds(verifyTokenLifetimeSec)));
			break;
		}
		}
		
		em.persist(token);
		
		return token;
	}
}
