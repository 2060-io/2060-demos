package io.gaiaid.registry.svc;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;
import org.jgroups.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.gaiaid.registry.enums.CreateStep;
import io.gaiaid.registry.enums.IdentityClaim;
import io.gaiaid.registry.enums.IssueStep;
import io.gaiaid.registry.enums.MediaType;
import io.gaiaid.registry.enums.Protection;
import io.gaiaid.registry.enums.RestoreStep;
import io.gaiaid.registry.enums.SessionType;
import io.gaiaid.registry.enums.TokenType;
import io.gaiaid.registry.ex.NoMediaException;
import io.gaiaid.registry.ex.TokenException;
import io.gaiaid.registry.jms.MtProducer;
import io.gaiaid.registry.model.Identity;
import io.gaiaid.registry.model.Media;
import io.gaiaid.registry.model.Session;
import io.gaiaid.registry.model.Token;
import io.gaiaid.registry.res.c.MediaResource;
import io.gaiaid.registry.res.c.Resource;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.Ciphering;
import io.twentysixty.sa.client.model.message.Claim;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.CredentialIssuanceMessage;
import io.twentysixty.sa.client.model.message.MediaItem;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
import io.twentysixty.sa.client.model.message.MenuItem;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.Parameters;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.Aes256cbc;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.CredentialTypeResource;



@ApplicationScoped
public class GaiaService {

	private static Logger logger = Logger.getLogger(GaiaService.class);

	@Inject EntityManager em;
	
	@RestClient
	@Inject MediaResource mediaResource;
	
	
	@Inject MtProducer mtProducer;
	
	@RestClient
	@Inject CredentialTypeResource credentialTypeResource;
	
	@ConfigProperty(name = "io.gaiaid.debug")
	Boolean debug;
	
	@ConfigProperty(name = "io.gaiaid.create.token.lifetimeseconds")
	Long createTokenLifetimeSec;
	
	@ConfigProperty(name = "io.gaiaid.verify.token.lifetimeseconds")
	Long verifyTokenLifetimeSec;
	
	@ConfigProperty(name = "io.gaiaid.protection")
	Protection protection;
	
	@ConfigProperty(name = "io.gaiaid.vision.face.capture.url")
	String faceCaptureUrl;
	
	@ConfigProperty(name = "io.gaiaid.vision.face.verification.url")
	String faceVerificationUrl;
	
	@ConfigProperty(name = "io.gaiaid.vision.fingerprints.capture.url")
	String fingerprintsCaptureUrl;
	
	@ConfigProperty(name = "io.gaiaid.vision.fingerprints.verification.url")
	String fingerprintsVerificationUrl;
	
	@ConfigProperty(name = "io.gaiaid.identity.recoverable.seconds")
	Long identityRecoverableSeconds;
	
	@ConfigProperty(name = "io.gaiaid.auth.valid.for.minutes")
	Integer authenticationValidForMinutes;
	
	// how will be named the credential
	@ConfigProperty(name = "io.gaiaid.identity.def.name")
	String defName;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.citizenid")
	Boolean enableCitizenIdClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.firstname")
	Boolean enableFirstnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.lastname")
	Boolean enableLastnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.avatarname")
	Boolean enableAvatarnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.avatarpic")
	Boolean enableAvatarpicClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.birthdate")
	Boolean enableBirthdateClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.birthplace")
	Boolean enableBirthplaceClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.def.claim.photo")
	Boolean enablePhotoClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.citizenid")
	Boolean restoreCitizenidClaim;
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.firstname")
	Boolean restoreFirstnameClaim;
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.lastname")
	Boolean restoreLastnameClaim;
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.avatarname")
	Boolean restoreAvatarnameClaim;
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.birthdate")
	Boolean restoreBirthdateClaim;
	@ConfigProperty(name = "io.gaiaid.identity.restore.claim.birthplace")
	Boolean restoreBirthplaceClaim;

	@ConfigProperty(name = "io.gaiaid.identity.def.claim.avatarpic.maxdimension")
	Integer avatarMaxDim;

	@ConfigProperty(name = "io.gaiaid.language")
	Optional<String> language;

	
	
	
	
	@ConfigProperty(name = "io.gaiaid.vision.redirdomain")
	Optional<String> redirDomain;
	
	@ConfigProperty(name = "io.gaiaid.vision.redirdomain.q")
	Optional<String> qRedirDomain;
	
	@ConfigProperty(name = "io.gaiaid.vision.redirdomain.d")
	Optional<String> dRedirDomain;
	
	@ConfigProperty(name = "io.gaiaid.messages.welcome")
	String WELCOME;
	
	@ConfigProperty(name = "io.gaiaid.messages.welcome2")
	Optional<String> WELCOME2;

	@ConfigProperty(name = "io.gaiaid.messages.welcome3")
	Optional<String> WELCOME3;

	@ConfigProperty(name = "io.gaiaid.messages.rootmenu.title")
	String ROOT_MENU_TITLE;
	
	
	
	
	//private static HashMap<UUID, SessionData> sessions = new HashMap<UUID, SessionData>();
	private static CredentialType type = null;
	private static Object lockObj = new Object();
	private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	
	//private static String ROOT_MENU_TITLE = "🌎 Gaia Identity Registry";
	//private static String ROOT_MENU_NO_SELECTED_ID_DESCRIPTION = "Use the contextual menu to select an Identity, or create a new one.";
	
	//private static String WELCOME = "Welcome to GIR (🌎 Gaia Identity Registry). Use the contextual menu to get started.";
	
	private static String CMD_SELECT_ID = "/select@";
	
	private static String CMD_CREATE = "/create";
	//private static String CMD_CREATE_LABEL = "Create a new Identity";
	
	private static String CMD_RESTORE = "/restore";
	//private static String CMD_RESTORE_LABEL = "Restore an Identity";
	
	
	private static String CMD_CREATE_ABORT = "/create_abort";
	//private static String CMD_CREATE_ABORT_LABEL = "Abort and return to main menu";
	private static String CMD_RESTORE_ABORT = "/restore_abort";
	//private static String CMD_RESTORE_ABORT_LABEL = "Abort and return to main menu";
	private static String CMD_EDIT_ABORT = "/edit_abort";
	//private static String CMD_EDIT_ABORT_LABEL = "Return to main menu";
	private static String CMD_VIEW_ID = "/view";
	//private static String CMD_VIEW_ID_LABEL = "View Identity";
	private static String CMD_UNDELETE = "/undelete";
	//private static String CMD_UNDELETE_LABEL = "Undelete this Identity";
	private static String CMD_ISSUE = "/issue";
	//private static String CMD_ISSUE_LABEL = "Issue Credential";
	private static String CMD_ISSUE_ABORT = "/issue_abort";
	//private static String CMD_ISSUE_ABORT_LABEL = "Abort and return to previous menu";
	
	private static String CMD_CONTINUE_SETUP = "/continue";
	//private static String CMD_CONTINUE_SETUP_LABEL = "Finish Identity Setup";
	
	
	private static String CMD_DELETE = "/delete";
	//private static String CMD_DELETE_LABEL = "Delete this Identity";
	private static String CMD_REVOKE = "/revoke";
	
	private static String COMPLETE_IDENTITY_CONFIRM_YES_VALUE = "CI_Yes";
	private static String COMPLETE_IDENTITY_CONFIRM_NO_VALUE = "CI_No";
	
		
	ResourceBundle bundle = null; 
	
	private String getMessage(String messageName) {
		String retval = messageName;
		if (bundle == null) {
			if (language.isPresent()) {
				try {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale(language.get())); 
				} catch (Exception e) {
					bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en")); 
				}
			} else {
				bundle = ResourceBundle.getBundle("META-INF/resources/Messages", new Locale("en")); 
			}
			
		}
		try {
			retval = bundle.getString(messageName);
		} catch (Exception e) {
			
		}
		
		
		return retval;
	}
	
	public BaseMessage getRootMenu(UUID connectionId, Session session, Identity identity) {
		
		
		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(ROOT_MENU_TITLE);
		List<ContextualMenuItem> options = new ArrayList<ContextualMenuItem>();

		if ((session == null) || (session.getType() == null)) {
			// main menu
			
			menu.setDescription(getMessage("ROOT_MENU_NO_SELECTED_ID_DESCRIPTION"));
			List<Identity> myIdentities = this.getMyIdentities(connectionId);
			int i = 0;
			
			if (myIdentities.size() != 0) {
				
				for (Identity currentIdentity: myIdentities) {
					i++;
					String label = this.getIdentityLabel(currentIdentity);
					String id = CMD_SELECT_ID + currentIdentity.getId();
					
					options.add(ContextualMenuItem.build(id, label, null));
				}
			}
			
			if (i<5) {
				// max 5 identities
				options.add(ContextualMenuItem.build(CMD_CREATE, getMessage("CMD_CREATE_LABEL"), null));
				options.add(ContextualMenuItem.build(CMD_RESTORE, getMessage("CMD_RESTORE_LABEL"), null));
			}
			
		} else switch (session.getType()) {
			
			
		case CREATE: {
				/* create menu */
				
				// abort and return to main menu
				options.add(ContextualMenuItem.build(CMD_CREATE_ABORT, getMessage("CMD_CREATE_ABORT_LABEL"), null));
				break;
		} 
		
		case RESTORE: {
				// restore menu
				// abort and return to main menu
				options.add(ContextualMenuItem.build(CMD_RESTORE_ABORT, getMessage("CMD_RESTORE_ABORT_LABEL"), null));
				break;
		} 
		case EDIT: {
				// edit menu
				// show identity in menu
				
				String idStr = getIdentityLabel(identity);
				menu.setDescription(idStr.toString());
				
				if (identity.getDeletedTs() != null) {
					options.add(ContextualMenuItem.build(CMD_VIEW_ID, getMessage("CMD_VIEW_ID_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_UNDELETE, getMessage("CMD_UNDELETE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				} else if (identity.getRevokedTs() != null) {
					options.add(ContextualMenuItem.build(CMD_VIEW_ID, getMessage("CMD_VIEW_ID_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_ISSUE, getMessage("CMD_ISSUE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_DELETE, getMessage("CMD_DELETE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				} else if (identity.getIssuedTs() != null) {
					options.add(ContextualMenuItem.build(CMD_VIEW_ID, getMessage("CMD_VIEW_ID_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_REVOKE, getMessage("CMD_REVOKE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				} else if (identity.getProtectedTs() == null) {
					options.add(ContextualMenuItem.build(CMD_CONTINUE_SETUP, getMessage("CMD_CONTINUE_SETUP_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_DELETE, getMessage("CMD_DELETE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				} else if (identity.getIssuedTs() == null) {
					options.add(ContextualMenuItem.build(CMD_VIEW_ID, getMessage("CMD_VIEW_ID_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_ISSUE, getMessage("CMD_ISSUE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_DELETE, getMessage("CMD_DELETE_LABEL"), null));
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				} else {
					options.add(ContextualMenuItem.build(CMD_EDIT_ABORT, getMessage("CMD_EDIT_ABORT_LABEL"), null));
				}
				break;
			}
		
		
		case ISSUE: {
			// edit menu
			// show identity in menu
			
			String idStr = getIdentityLabel(identity);
			menu.setDescription(idStr.toString());
			options.add(ContextualMenuItem.build(CMD_ISSUE_ABORT, getMessage("CMD_ISSUE_ABORT_LABEL"), null));
			
		}
		default: {
			break;
		}
		}
			
		
		menu.setOptions(options);
		


		if (debug) {
			try {
				logger.info("getRootMenu: " + JsonUtil.serialize(menu, false));
			} catch (JsonProcessingException e) {
			}
		}
		menu.setConnectionId(connectionId);
		menu.setId(UUID.randomUUID());
		menu.setTimestamp(Instant.now());
		
		return menu;
		

	}
	
	private String getIdentityLabel(Identity identity) {
		StringBuffer idLabel = new StringBuffer(64);
		
		if (identity.getDeletedTs() != null) {
			idLabel.append("🧨");
		} else if (identity.getRevokedTs() != null) {
			idLabel.append("❌");
		} else if (identity.getIssuedTs() != null) {
			if (identity.getProtectedTs() == null) {
				idLabel.append("⚠️ (unprotected)");
			} else {
				idLabel.append("✅");
			}
			
		} else if (identity.getProtectedTs() != null) {
			idLabel.append("🔐");
		} else {
			idLabel.append("✏️️");
		}
		
		boolean name = false;
		if (identity.getFirstname() != null) {
			idLabel.append(" ").append(identity.getFirstname());
			name = true;
		}
		if (identity.getLastname() != null) {
			idLabel.append(" ").append(identity.getLastname());
			name = true;
		}
		if (identity.getAvatarname() != null) {
			idLabel.append(" ").append(identity.getAvatarname());
			name = true;
		}
		if (!name) {
			
			if (identity.getCitizenId() != null) {
				idLabel.append(" ").append(identity.getCitizenId());
			} else {
				idLabel.append(" ").append(" <unset name>");
			}
			
			
		}
		
		return idLabel.toString();
	}
	
	
	@Transactional
	public List<Identity> getMyIdentities(UUID connectionId) {
		Query q = em.createNamedQuery("Identity.findForConnection");
		q.setParameter("connectionId", connectionId);
		q.setParameter("deletedTs", Instant.now().minusSeconds(identityRecoverableSeconds));
		return q.getResultList();
	}
	
	
	@Transactional
	public void userInput(BaseMessage message) throws Exception {
		
		
		String content = null;

		MediaMessage mm = null;
		
		if (message instanceof TextMessage) {
			
			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {
			
			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();
			
		} else if ((message instanceof MenuSelectMessage)) {
			
			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		} else if ((message instanceof MediaMessage)) {
			mm = (MediaMessage) message;
			content = "media";
		}
		
		if (content != null) {
			content = content.strip();
			if (content.length()==0) content = null;
		}
		
		if (content == null) return;
		
		Session session = this.getSession(message.getConnectionId());
		Identity identity = null;
		
		if (session != null) {
			identity = session.getIdentity();
			
			try {
				logger.info("userInput: session: " + JsonUtil.serialize(session, false));
			} catch (JsonProcessingException e) {
				
			}
			
			
		}
		
		if (identity != null) {
			try {
				logger.info("userInput: identity: " + JsonUtil.serialize(identity, false));
				
			} catch (JsonProcessingException e) {
				
			}
		}
		
		
		UUID identityId = null;
		
		if (content.startsWith(CMD_SELECT_ID)) {
			
			logger.info("userInput: CMD_SELECT_ID : session before: " + session);
			
			
			String[] ids = content.split("@");
			boolean found = false;
			if ((ids != null) && (ids.length>1)) {
				if (ids[1] != null) {
					try {
						identityId = UUID.fromString(ids[1]);
					} catch (Exception e) {
					}
					if (identityId != null) {
						identity = em.find(Identity.class, identityId);
						if (identity != null) {
							session = createSession(session, message.getConnectionId());
							session.setConnectionId(message.getConnectionId());
							session.setType(SessionType.EDIT);
							session.setIdentity(identity);
							session = em.merge(session);
							mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_SELECTED").replace("IDENTITY", this.getIdentityLabel(identity))));
							found = true;
						}
					}
				}
			}
			if (!found) {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_NOT_FOUND")));
				em.remove(session);
				session = null;
			}
			
		} else if (content.equals(CMD_CREATE)) {
			logger.info("userInput: CMD_CREATE : session before: " + session);
			
			session = createSession(session, message.getConnectionId());
			session.setType(SessionType.CREATE);
			session = em.merge(session);
			
			this.createEntryPoint(message.getConnectionId(), message.getThreadId(), session, null, null);
			
		} else if (content.equals(CMD_RESTORE)) {
			
			logger.info("userInput: CMD_RESTORE : session before: " + session);
			
			session = createSession(session, message.getConnectionId());
			session.setType(SessionType.RESTORE);
			session = em.merge(session);
			this.restoreEntryPoint(message.getConnectionId(), message.getThreadId(), session, null);
			

		} 
		
		else if (content.equals(CMD_CONTINUE_SETUP)) {
			logger.info("userInput: CMD_CONTINUE_SETUP : session before: " + session);
			
			if (session != null) {
				session.setType(SessionType.CREATE);
				
				session.setCreateStep(CreateStep.CAPTURE);
				session = em.merge(session);
				
				this.createEntryPoint(message.getConnectionId(), message.getThreadId(), session, null, null);
			}
			
			logger.info("userInput: CMD_CONTINUE_SETUP : session after: " + session);
		} 
		
		else if (content.equals(CMD_CREATE_ABORT)) {
			logger.info("userInput: CMD_CREATE_ABORT : session before: " + session);
			
			if (session != null) {
				em.remove(session);
				session = null;
			}
			mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_CREATE_ABORTED")));

			logger.info("userInput: CMD_CREATE_ABORT : session after: " + session);
		} else if (content.equals(CMD_RESTORE_ABORT)) {
			logger.info("userInput: CMD_RESTORE_ABORT : session before: " + session);
			if (session != null) {
				em.remove(session);
				session = null;
			}
			mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_RESTORE_ABORTED")));

		} else if (content.equals(CMD_VIEW_ID)) {
			logger.info("userInput: CMD_VIEW_ID : session before: " + session);
			if ( (session != null)  && (session.getType() != null) && (session.getType().equals(SessionType.EDIT)) && (identity != null)) {
				
				String idstr = this.getIdentityDataString(identity);
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), idstr));
			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_SELECT_IDENTITY_FIRST")));
			}
			
		} else if (content.equals(CMD_UNDELETE)) {
			logger.info("userInput: CMD_UNDELETE : session before: " + session);
			if ( (session != null)  && (session.getType() != null) && (session.getType().equals(SessionType.EDIT)) && (identity != null)) {
				if (identity.getDeletedTs().plusSeconds(86400).compareTo(Instant.now())>0) {
					identity.setDeletedTs(null);
					identity = em.merge(identity);
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_UNDELETED").replace("IDENTITY", this.getIdentityLabel(identity))));

				} else {
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_IDENTITY_UNDELETE").replace("IDENTITY", this.getIdentityLabel(identity))));
				}
			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_SELECT_IDENTITY_FIRST")));
			}
			
			
		} else if (content.equals(CMD_EDIT_ABORT)) {
			logger.info("userInput: CMD_EDIT_ABORT : session before: " + session);
			if (session != null) {
				em.remove(session);
				session = null;
			}
			mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_EDIT_ABORTED")));

		} else if (content.equals(CMD_ISSUE)) {
			logger.info("userInput: CMD_ISSUE : session before: " + session);
			if ( (session != null)  && (session.getType() != null) && (session.getType().equals(SessionType.EDIT)) && (identity != null)) {
				
				session.setType(SessionType.ISSUE);
				session.setIdentity(identity);
				session = em.merge(session);
				this.issueEntryPoint(message.getConnectionId(), message.getThreadId(), session, null);
				
			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_SELECT_IDENTITY_FIRST")));
			}
			
		} else if (content.equals(CMD_ISSUE_ABORT)) {
			logger.info("userInput: CMD_ISSUE_ABORT : session before: " + session);
			if (session != null) {
				session.setType(SessionType.EDIT);
				session = em.merge(session);
				
			}
			mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_ISSUANCE_ABORTED")));

		} else if (content.equals(CMD_DELETE)) {
			logger.info("userInput: CMD_DELETE : session before: " + session);
			
			if ( (session != null)  && (session.getType() != null) && (session.getType().equals(SessionType.EDIT)) && (identity != null)) {
				if ( (identity.getIssuedTs() == null)
				|| (identity.getRevokedTs() != null)
						)
				{
					
					identity.setDeletedTs(Instant.now());
					em.merge(identity);
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_DELETED").replace("IDENTITY", this.getIdentityLabel(identity))));
				}
				
			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_SELECT_IDENTITY_FIRST")));
			}
			
			
			
		} else if (content.equals(CMD_REVOKE)) {
			logger.info("userInput: CMD_REVOKE : session before: " + session);
			if ( (session != null)  && (session.getType() != null) && (session.getType().equals(SessionType.EDIT)) && (identity != null)) {
				if (identity.getIssuedTs() != null) {
					identity.setRevokedTs(Instant.now());
					identity = em.merge(identity);
					mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("IDENTITY_REVOKED").replace("IDENTITY", this.getIdentityLabel(identity))));
				}
			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("ERROR_SELECT_IDENTITY_FIRST")));
			}
			
		} else if ((session != null) && (session.getType()!= null) && (session.getType().equals(SessionType.CREATE))) {
			logger.info("userInput: CREATE entryPoint session before: " + session);
			
			this.createEntryPoint(message.getConnectionId(), message.getThreadId(), session, content, mm);
		} else if ((session != null) && (session.getType()!= null) && (session.getType().equals(SessionType.RESTORE))) {
			logger.info("userInput: RESTORE entryPoint session before: " + session);
			
			this.restoreEntryPoint(message.getConnectionId(), message.getThreadId(), session, content);
		} else if ((session != null) && (session.getType()!= null) && (session.getType().equals(SessionType.ISSUE))) {
			logger.info("userInput: ISSUE entryPoint session before: " + session);
			
			this.issueEntryPoint(message.getConnectionId(), message.getThreadId(), session, content);
		} else {
			mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getMessage("HELP")));
		}
		
		logger.info("userInput: sending menu content: " + content + " session: " + session + " identity: " + identity  );
		
		mtProducer.sendMessage(this.getRootMenu(message.getConnectionId(), session, identity));
	}
	
	// ?token=TOKEN&d=D_DOMAIN&q=Q_DOMAIN
	private String buildVisionUrl(String url) {
		
		if(redirDomain.isPresent()) {
			url = url + "&rd=" +  redirDomain.get();
		}
		if(qRedirDomain.isPresent()) {
			url = url + "&q=" +  qRedirDomain.get();
		}
		if(dRedirDomain.isPresent()) {
			url = url + "&d=" +  dRedirDomain.get();
		}
		if (language.isPresent()) {
			url = url + "&lang=" +  language.get();
		}
		
		return url;
	}
	
	
	private BaseMessage generateFaceVerificationMediaMessage(UUID connectionId, UUID threadId, Token token) {
		String url = faceVerificationUrl.replaceFirst("TOKEN", token.getId().toString());
		url = this.buildVisionUrl(url);
		
		MediaItem mi = new MediaItem();
		mi.setMimeType("text/html");
		mi.setUri(url);
		mi.setTitle(getMessage("FACE_VERIFICATION_HEADER"));
		mi.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mi.setOpeningMode("normal");
		List<MediaItem> mis = new ArrayList<MediaItem>();
		mis.add(mi);
		MediaMessage mm = new MediaMessage();
		mm.setConnectionId(connectionId);
		mm.setThreadId(threadId);
		mm.setDescription(getMessage("FACE_VERIFICATION_DESC"));
		mm.setItems(mis);
		return mm;
	}
	
	private BaseMessage generateFaceCaptureMediaMessage(UUID connectionId, UUID threadId, Token token) {
		String url = faceCaptureUrl.replaceFirst("TOKEN", token.getId().toString());
		url = this.buildVisionUrl(url);
		MediaItem mi = new MediaItem();
		mi.setMimeType("text/html");
		mi.setUri(url);
		mi.setTitle(getMessage("FACE_CAPTURE_HEADER"));
		mi.setDescription(getMessage("FACE_CAPTURE_DESC"));
		mi.setOpeningMode("normal");
		List<MediaItem> mis = new ArrayList<MediaItem>();
		mis.add(mi);
		MediaMessage mm = new MediaMessage();
		mm.setConnectionId(connectionId);
		mm.setThreadId(threadId);
		mm.setDescription(getMessage("FACE_CAPTURE_DESC"));
		mm.setItems(mis);
		return mm;
	}

	private void restoreEntryPoint(UUID connectionId, UUID threadId, Session session, String content) throws Exception {
		
		
		if (session.getRestoreStep() == null) {
			session.setRestoreStep(getNextRestoreStep(null));
			session = em.merge(session);
			this.restoreSendMessage(connectionId, threadId, session);
		} else switch (session.getRestoreStep()) {
		
		case CITIZEN_ID: {
			if (content != null) {
				
				try {
					// parse long
					Long citizenId = Long.valueOf(content);
					if(citizenId<0) citizenId=-citizenId;
					session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
					session.setCitizenId(citizenId.toString());
					session = em.merge(session);
				} catch (Exception e) {
					logger.error("", e);
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("ID_ERROR")));
				}
				
			}
			this.restoreSendMessage(connectionId, threadId, session);
			break;
		}
		
			case FIRSTNAME: {
				if (content != null) {
					session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
					session.setFirstname(content);
					session = em.merge(session);
				}
				this.restoreSendMessage(connectionId, threadId, session);
				break;
			} 
			case LASTNAME: {
				if (content != null) {
					session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
					session.setLastname(content);
					session = em.merge(session);
				}
				this.restoreSendMessage(connectionId, threadId, session);
				break;
			} 
			
			case AVATARNAME: {
				if (content != null) {
					session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
					session.setAvatarname(content);
					session = em.merge(session);
				}
				this.restoreSendMessage(connectionId, threadId, session);
				break;
			} 
			
			case BIRTHDATE: {
				if (content != null) {
					
					LocalDate birthDate = null;
					
					try {
						// parse date
						birthDate = LocalDate.from(df.parse(content));
						session.setBirthdate(birthDate);
						session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
						session = em.merge(session);
						
						this.restoreSendMessage(connectionId, threadId, session);
						
					} catch (Exception e) {
						logger.error("", e);
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("BIRTHDATE_ERROR")));
						this.restoreSendMessage(connectionId, threadId, session);
					}
					
					
				} else {
					this.restoreSendMessage(connectionId, threadId, session);
				}
				
				break;
			} 
			
			case PLACE_OF_BIRTH: {
				if (content != null) {
					session.setRestoreStep(getNextRestoreStep(session.getRestoreStep()));
					session.setPlaceOfBirth(content);
					session = em.merge(session);
					
					
				}
				
				this.restoreSendMessage(connectionId, threadId, session);
				
				break;
			} 
			
			case FACE_VERIFICATION: {
				
				Token token = this.getToken(connectionId, TokenType.FACE_VERIFICATION, session.getIdentity());
				mtProducer.sendMessage(generateFaceVerificationMediaMessage(connectionId, threadId, token));
				/*mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_VERIFICATION_REQUEST.replaceFirst("URL", faceVerificationUrl.replaceFirst("TOKEN", token.getId().toString())
						.replaceFirst("REDIRDOMAIN", redirDomain)
						.replaceFirst("Q_DOMAIN", qRedirDomain)
						.replaceFirst("D_DOMAIN", dRedirDomain)
						)));
				*/
				break;
			}
			
			case FINGERPRINT_VERIFICATION: {
				Token token = this.getToken(connectionId, TokenType.FINGERPRINT_VERIFICATION, session.getIdentity());
				
				
				break;
			}
			case PASSWORD: {
				if (content != null) {
					logger.info("restoreEntryPoint: password: " + content);
					String password = DigestUtils.sha256Hex(content);
					// 	@NamedQuery(name="Identity.findForRestore", query="SELECT i FROM Identity i where i.connectionId<>:connectionId and (i.deletedTs IS NULL or i.deletedTs>:deletedTs) and i.firstname=:firstname and i.lastname=:lastname and i.birthdate=:birthdate  ORDER by i.id ASC"),
					
					Identity identity = session.getIdentity();
					
					boolean restored = false;
					
					if (identity.getPassword() != null) {
						if (identity.getPassword().equals(password)) {
							
							identity.setConnectionId(connectionId);
							identity.setAuthenticatedTs(Instant.now());
							identity = em.merge(identity);
							mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("AUTHENTICATION_SUCCESSFULL")));
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_RESTORED").replaceFirst("IDENTITY", this.getIdentityLabel(identity))));
							session = this.issueCredentialAndSetEditMenu(session, identity);
							
							restored = true;
						}
					}
					
					if (debug) {
						logger.info("entryPointRestore: finding Identity with firstname: " + session.getFirstname()
								+ " lastname: " + session.getLastname()
								+ " birthdate: " + session.getBirthdate()
								+ " password: " + password
								);
					}
					
					if (!restored) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_NOT_FOUND")));
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_PASSWORD")));
					}
					
					
				} else {
					this.restoreSendMessage(connectionId, threadId, session);
				}
				break;
				
			
			} 
			
			
		}
		
		if ((session.getRestoreStep() != null) && (session.getRestoreStep().equals(RestoreStep.DONE))) {
			
			
			
			
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Identity> query = builder.createQuery(Identity.class);
			Root<Identity> root = query.from(Identity.class);
			
			List<Predicate> allPredicates = new ArrayList<Predicate>();
				
			if (restoreCitizenidClaim) {
				Predicate predicate = builder.equal(root.get("citizenId"), session.getCitizenId());
				allPredicates.add(predicate);
			}
			
			if (restoreFirstnameClaim) {
				Predicate predicate = builder.equal(root.get("firstname"), session.getFirstname());
				allPredicates.add(predicate);
			}
			if (restoreLastnameClaim) {
				Predicate predicate = builder.equal(root.get("lastname"), session.getLastname());
				allPredicates.add(predicate);
			}
			if (restoreAvatarnameClaim) {
				Predicate predicate = builder.equal(root.get("avatarname"), session.getAvatarname());
				allPredicates.add(predicate);
			}
			
			if (restoreBirthdateClaim) {
				Predicate predicate = builder.equal(root.get("birthdate"), session.getBirthdate());
				allPredicates.add(predicate);
			}
			if (restoreBirthplaceClaim) {
				Predicate predicate = builder.equal(root.get("placeOfBirth"), session.getPlaceOfBirth());
				allPredicates.add(predicate);
			}
			
			query.where(builder.and(allPredicates.toArray(new Predicate[allPredicates.size()])));
			
			query.orderBy(builder.desc(root.get("id")));
			Query q = em.createQuery(query);
			
			Identity res = (Identity) q.getResultList().stream().findFirst().orElse(null);
			
			if (res != null) {
				if (res.getConnectionId() != null) {
					if (res.getConnectionId().equals(connectionId)) {
						res = null;
					}
				}
			}
			if (res == null) {
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_NOT_FOUND")));
				this.purgeSession(session);
				session.setType(SessionType.RESTORE);
				session.setRestoreStep(getNextRestoreStep(null));
				session = em.merge(session);
				this.restoreSendMessage(connectionId, threadId, session);
			} else {
				session.setIdentity(res);
				switch (res.getProtection()) {
				case PASSWORD: {
					
					logger.info("restoreEntryPoint: found password method for identity " + this.getIdentityDataString(res));
					session.setRestoreStep(RestoreStep.PASSWORD);
					session = em.merge(session);
					this.restoreSendMessage(connectionId, threadId, session);
					break;
				}
				
				case FACE: {
					logger.info("restoreEntryPoint: found face verification method for identity " + this.getIdentityDataString(res));
					
					session.setRestoreStep(RestoreStep.FACE_VERIFICATION);
					session = em.merge(session);
					
					Token token = this.getToken(connectionId, TokenType.FACE_VERIFICATION, res);
					
					
					mtProducer.sendMessage(generateFaceVerificationMediaMessage(connectionId, threadId, token));
					/*mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_VERIFICATION_REQUEST.replaceFirst("URL", faceVerificationUrl.replaceFirst("TOKEN", token.getId().toString())
							.replaceFirst("REDIRDOMAIN", redirDomain)
							.replaceFirst("Q_DOMAIN", qRedirDomain)
							.replaceFirst("D_DOMAIN", dRedirDomain)
							
							)));*/
					
					logger.info("restoreEntryPoint: session: " + JsonUtil.serialize(session, false));

					break;
				}
				
				case FINGERPRINTS: {
					
					logger.info("restoreEntryPoint: found fingerprint verification method for identity " + this.getIdentityDataString(res));
					
					session.setRestoreStep(RestoreStep.FINGERPRINT_VERIFICATION);
					session = em.merge(session);
					
					Token token = this.getToken(connectionId, TokenType.FINGERPRINT_VERIFICATION, res);
					
					
					break;
					
					
				}
					
				}
			}
		}
	}
	
	
	private void restoreSendMessage(UUID connectionId, UUID threadId, Session session) throws Exception {
		
		if (session.getRestoreStep() == null) {
			
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_FIRSTNAME")));
		} 
		
		else switch (session.getRestoreStep()) {
		
		case CITIZEN_ID:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_CITIZEN_ID")));
			break;
		} 
		
		case FIRSTNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_FIRSTNAME")));
			break;
		} 
		case LASTNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_LASTNAME")));
			break;
		}
		case AVATARNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_AVATARNAME")));
			break;
		}
		case BIRTHDATE: {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_BIRTHDATE")));
			break;
		}
		case PLACE_OF_BIRTH: {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_PLACE_OF_BIRTH")));
			break;
		}
		
		case PASSWORD: {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("RESTORE_PASSWORD")));
			break;
		}
		
		case FACE_VERIFICATION:
		case FINGERPRINT_VERIFICATION:
		default:
		{
			
			break;
		}
		
		}
	}

	
	

	
	private boolean identityAlreadyExists(Session session) {
		
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Identity> query = builder.createQuery(Identity.class);
		Root<Identity> root = query.from(Identity.class);
		
		List<Predicate> allPredicates = new ArrayList<Predicate>();
			
		if (restoreCitizenidClaim) {
			Predicate predicate = builder.equal(root.get("citizenId"), session.getCitizenId());
			allPredicates.add(predicate);
			if (debug) logger.info("identityAlreadyExists: citizenId: " + session.getCitizenId());
		}
		
		if (restoreFirstnameClaim) {
			Predicate predicate = builder.equal(root.get("firstname"), session.getFirstname());
			allPredicates.add(predicate);
			
			if (debug) logger.info("identityAlreadyExists: firstname: " + session.getFirstname());
		}
		if (restoreLastnameClaim) {
			Predicate predicate = builder.equal(root.get("lastname"), session.getLastname());
			allPredicates.add(predicate);
			if (debug) logger.info("identityAlreadyExists: lastname: " + session.getLastname());
		}
		if (restoreAvatarnameClaim) {
			Predicate predicate = builder.equal(root.get("avatarname"), session.getAvatarname());
			allPredicates.add(predicate);
			if (debug) logger.info("identityAlreadyExists: avatarname: " + session.getAvatarname());
		}
		
		if (restoreBirthdateClaim) {
			Predicate predicate = builder.equal(root.get("birthdate"), session.getBirthdate());
			allPredicates.add(predicate);
			if (debug) logger.info("identityAlreadyExists: birthdate: " + session.getBirthdate());
		}
		if (restoreBirthplaceClaim) {
			Predicate predicate = builder.equal(root.get("placeOfBirth"), session.getPlaceOfBirth());
			allPredicates.add(predicate);
			if (debug) logger.info("identityAlreadyExists: placeOfBirth: " + session.getPlaceOfBirth());
		}
		
		query.where(builder.and(allPredicates.toArray(new Predicate[allPredicates.size()])));
		
		query.orderBy(builder.desc(root.get("id")));
		Query q = em.createQuery(query);
		
		List<Identity> founds = q.getResultList();
		if (debug) {
			try {
				logger.info("identityAlreadyExists: found: " + JsonUtil.serialize(founds, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		
		return (founds.size()>0);
		
	}
	
	
	/*
	 * @ConfigProperty(name = "io.gaiaid.identity.claims.idnumber")
	Boolean enableIdNumberClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.firstname")
	Boolean enableFirstnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.lastname")
	Boolean enableLastnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.avatarname")
	Boolean enableAvatarnameClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.avatarpic")
	Boolean enableAvatarPicClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.birthdate")
	Boolean enableBirthdateClaim;
	
	@ConfigProperty(name = "io.gaiaid.identity.claims.birthplace")
	Boolean enableBirthplaceClaim;
	
	 */
	
	private CreateStep getNextCreateStep(CreateStep current) throws Exception {
		
		if (current == null) {
			if(enableCitizenIdClaim) return CreateStep.CITIZEN_ID;
			if(enableFirstnameClaim) return CreateStep.FIRSTNAME;
			if(enableLastnameClaim) return CreateStep.LASTNAME;
			if(enableAvatarnameClaim) return CreateStep.AVATARNAME;
			if(enableAvatarpicClaim) return CreateStep.AVATARPIC;
			if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
			if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
			
			throw new Exception("no claim has been enabled");
		} else {
			switch (current) {
			case CITIZEN_ID: {
				if(enableFirstnameClaim) return CreateStep.FIRSTNAME;
				if(enableLastnameClaim) return CreateStep.LASTNAME;
				if(enableAvatarnameClaim) return CreateStep.AVATARNAME;
				if(enableAvatarpicClaim) return CreateStep.AVATARPIC;
				if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case FIRSTNAME: {
				if(enableLastnameClaim) return CreateStep.LASTNAME;
				if(enableAvatarnameClaim) return CreateStep.AVATARNAME;
				if(enableAvatarpicClaim) return CreateStep.AVATARPIC;
				if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case LASTNAME: {
				if(enableAvatarnameClaim) return CreateStep.AVATARNAME;
				if(enableAvatarpicClaim) return CreateStep.AVATARPIC;
				if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case AVATARNAME: {
				if(enableAvatarpicClaim) return CreateStep.AVATARPIC;
				if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case AVATARPIC: {
				if(enableBirthdateClaim) return CreateStep.BIRTHDATE;
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case BIRTHDATE: {
				if(enableBirthplaceClaim) return CreateStep.PLACE_OF_BIRTH;
				return CreateStep.PENDING_CONFIRM;
			}
			case PLACE_OF_BIRTH:
			default:
			{
				return CreateStep.PENDING_CONFIRM;
			}
			}
		}
		
		
	}
	
	private RestoreStep getNextRestoreStep(RestoreStep current) throws Exception {
		
		if (current == null) {
			if(restoreCitizenidClaim) return RestoreStep.CITIZEN_ID;
			if(restoreFirstnameClaim) return RestoreStep.FIRSTNAME;
			if(restoreLastnameClaim) return RestoreStep.LASTNAME;
			if(restoreAvatarnameClaim) return RestoreStep.AVATARNAME;
			if(restoreBirthdateClaim) return RestoreStep.BIRTHDATE;
			if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
			
			throw new Exception("no claim has been enabled");
		} else {
			switch (current) {
			case CITIZEN_ID: {
				
				if(restoreFirstnameClaim) return RestoreStep.FIRSTNAME;
				if(restoreLastnameClaim) return RestoreStep.LASTNAME;
				if(restoreAvatarnameClaim) return RestoreStep.AVATARNAME;
				if(restoreBirthdateClaim) return RestoreStep.BIRTHDATE;
				if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
				return RestoreStep.DONE;
			}
			case FIRSTNAME: {
				if(restoreLastnameClaim) return RestoreStep.LASTNAME;
				if(restoreAvatarnameClaim) return RestoreStep.AVATARNAME;
				if(restoreBirthdateClaim) return RestoreStep.BIRTHDATE;
				if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
				return RestoreStep.DONE;
			}
			case LASTNAME: {
				if(restoreAvatarnameClaim) return RestoreStep.AVATARNAME;
				if(restoreBirthdateClaim) return RestoreStep.BIRTHDATE;
				if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
				return RestoreStep.DONE;
			}
			case AVATARNAME: {
				if(restoreBirthdateClaim) return RestoreStep.BIRTHDATE;
				if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
				return RestoreStep.DONE;
			}
			
			case BIRTHDATE: {
				if(restoreBirthplaceClaim) return RestoreStep.PLACE_OF_BIRTH;
				return RestoreStep.DONE;
			}
			case PLACE_OF_BIRTH:
			default:
			{
				return RestoreStep.DONE;
			}
			}
		}
		
		
	}
	
	private void createEntryPoint(UUID connectionId, UUID threadId, Session session, String content, MediaMessage mm) throws Exception {
		
		if (session.getCreateStep() == null) {
			session.setCreateStep(getNextCreateStep(null));
			session = em.merge(session);
			this.createSendMessage(connectionId, threadId, session);
			
		} else switch (session.getCreateStep()) {
		case CITIZEN_ID: {
			if (content != null) {
				
				try {
					// parse long
					Long citizenId = Long.valueOf(content);
					if(citizenId<0) citizenId=-citizenId;
					session.setCreateStep(getNextCreateStep(session.getCreateStep()));
					session.setCitizenId(citizenId.toString());
					
				} catch (Exception e) {
					logger.error("", e);
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("ID_ERROR")));
				}
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		} 
		case FIRSTNAME: {
			if (content != null) {
				session.setCreateStep(getNextCreateStep(session.getCreateStep()));
				session.setFirstname(content);
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		} 
		case LASTNAME: {
			if (content != null) {
				session.setCreateStep(getNextCreateStep(session.getCreateStep()));
				session.setLastname(content);
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		case AVATARNAME: {
			if (content != null) {
				session.setCreateStep(getNextCreateStep(session.getCreateStep()));
				session.setAvatarname(content);
				
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		case AVATARPIC: {
			if (mm != null) {
				
				this.saveAvatarPicture(mm, session);
				
				if (session.getAvatarPic() != null) {
					session.setCreateStep(getNextCreateStep(session.getCreateStep()));
					
					if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
						
						if (this.identityAlreadyExists(session)) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
							if (session.getAvatarPic() == null) {
								mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
							} else {
								MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
								mtProducer.sendMessage(mms);
								
							}
							session.setCreateStep(CreateStep.NEED_TO_CHANGE);
						} 
					}
					session = em.merge(session);
				}
 				
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		case BIRTHDATE: {
			if (content != null) {
				
				LocalDate birthDate = null;
				
				try {
					// parse date
					birthDate = LocalDate.from(df.parse(content));
					session.setBirthdate(birthDate);
					session.setCreateStep(getNextCreateStep(session.getCreateStep()));
					session = em.merge(session);
				} catch (Exception e) {
					logger.error("", e);
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("BIRTHDATE_ERROR")));
				}
				
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				session = em.merge(session);
			}
			
			
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		case PLACE_OF_BIRTH: {
			if (content != null) {
				session.setPlaceOfBirth(content);
				session.setCreateStep(getNextCreateStep(session.getCreateStep()));
				
				if (session.getCreateStep().equals(CreateStep.PENDING_CONFIRM)) {
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} 
				}
				
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		
		case PENDING_CONFIRM: {
			
			Identity identity = null;
			if (content != null) {
				if (content.equals(COMPLETE_IDENTITY_CONFIRM_YES_VALUE)) {
					
					if (!this.identityAlreadyExists(session)) {
						identity = new Identity();
						identity.setId(UUID.randomUUID());
						identity.setCitizenId(session.getCitizenId());
						identity.setFirstname(session.getFirstname());
						identity.setLastname(session.getLastname());
						identity.setAvatarPic(session.getAvatarPic());
						identity.setAvatarPicCiphAlg(session.getAvatarPicCiphAlg());
						identity.setAvatarPicCiphIv(session.getAvatarPicCiphIv());
						identity.setAvatarPicCiphKey(session.getAvatarPicCiphKey());
						identity.setAvatarMimeType(session.getAvatarMimeType());
						identity.setAvatarname(session.getAvatarname());
						identity.setBirthdate(session.getBirthdate());
						identity.setPlaceOfBirth(session.getPlaceOfBirth());
						identity.setCitizenSinceTs(Instant.now());
						identity.setConnectionId(connectionId);
						identity.setProtection(protection);
						em.persist(identity);
						
						session.setIdentity(identity);
						session = em.merge(session);
					} 
					
					session.setCreateStep(CreateStep.CAPTURE);
					session = em.merge(session);
					
					
				} else if (content.equals(COMPLETE_IDENTITY_CONFIRM_NO_VALUE)) {
					session.setCreateStep(CreateStep.WANT_TO_CHANGE);
					session = em.merge(session);
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					this.createSendMessage(connectionId, threadId, session);
					break;
				} else {
					this.createSendMessage(connectionId, threadId, session);
					break;
				}
			} else {
				this.createSendMessage(connectionId, threadId, session);
				break;
			}
			
		}
		case CAPTURE: {
			Identity identity = session.getIdentity();
			if ((identity != null) && (identity.getProtectedTs() == null))	{
				
					switch (protection) {
					case PASSWORD: {
						session.setCreateStep(CreateStep.PASSWORD);
						session = em.merge(session);
						
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_REQUEST")));
						
						break;
					}
					case FACE: {
						
						session.setCreateStep(CreateStep.FACE_CAPTURE);
						session = em.merge(session);
						
						Token token = this.getToken(connectionId, TokenType.FACE_CAPTURE, session.getIdentity());
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("FACE_CAPTURE_REQUIRED")));
						mtProducer.sendMessage(generateFaceCaptureMediaMessage(connectionId, threadId, token));
						/*
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_CAPTURE_REQUEST.replaceFirst("URL", faceCaptureUrl.replaceFirst("TOKEN", token.getId().toString())
								.replaceFirst("REDIRDOMAIN", redirDomain)
								.replaceFirst("Q_DOMAIN", qRedirDomain)
								.replaceFirst("D_DOMAIN", dRedirDomain)
								)));
						*/
						break;
					}
					case FINGERPRINTS: {
						
						session.setCreateStep(CreateStep.FINGERPRINT_CAPTURE);
						session = em.merge(session);
						
						Token token = this.getToken(connectionId, TokenType.FINGERPRINT_CAPTURE, session.getIdentity());
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("FINGERPRINT_CAPTURE_REQUIRED")));
					/*	mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("FINGERPRINT_CAPTURE_REQUEST").replaceFirst("URL", fingerprintsCaptureUrl.replaceFirst("TOKEN", token.getId().toString())
								.replaceFirst("REDIRDOMAIN", redirDomain)
								.replaceFirst("Q_DOMAIN", qRedirDomain)
								.replaceFirst("D_DOMAIN", dRedirDomain))));*/
						
						break;
					}
					}
					
				}  else {
					this.purgeSession(session);
					
				}
			
			
			break;
		}
		case WANT_TO_CHANGE:
		case NEED_TO_CHANGE:
		{
			if (content != null) {
				if (content.equals(IdentityClaim.CITIZEN_ID.toString())) {
					session.setCreateStep(CreateStep.CHANGE_CITIZEN_ID);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.FIRSTNAME.toString())) {
					session.setCreateStep(CreateStep.CHANGE_FIRSTNAME);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.LASTNAME.toString())) {
					session.setCreateStep(CreateStep.CHANGE_LASTNAME);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.AVATARNAME.toString())) {
					session.setCreateStep(CreateStep.CHANGE_AVATARNAME);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.AVATARPIC.toString())) {
					session.setCreateStep(CreateStep.CHANGE_AVATARPIC);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.BIRTHDATE.toString())) {
					session.setCreateStep(CreateStep.CHANGE_BIRTHDATE);
					session = em.merge(session);
				} else if (content.equals(IdentityClaim.PLACE_OF_BIRTH.toString())) {
					session.setCreateStep(CreateStep.CHANGE_PLACE_OF_BIRTH);
					session = em.merge(session);
				} 
			} 
			this.createSendMessage(connectionId, threadId, session);
			
			break;
		}
		
		case CHANGE_CITIZEN_ID: {
			if (content != null) {
				
				
				if (content != null) {
					
					try {
						// parse long
						Long citizenId = Long.valueOf(content);
						if(citizenId<0) citizenId=-citizenId;
						session.setCitizenId(citizenId.toString());
					} catch (Exception e) {
						logger.error("", e);
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("ID_ERROR")));
					}
					
				}
				
				if (this.identityAlreadyExists(session)) {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					session.setCreateStep(CreateStep.NEED_TO_CHANGE);
				} else {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
				}
				
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		
		case CHANGE_FIRSTNAME: {
			if (content != null) {
				session.setCreateStep(CreateStep.PENDING_CONFIRM);
				session.setFirstname(content);
				
				if (this.identityAlreadyExists(session)) {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					session.setCreateStep(CreateStep.NEED_TO_CHANGE);
				} else {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
				}
				
				session = em.merge(session);
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		
		case CHANGE_LASTNAME: {
			if (content != null) {
				session.setCreateStep(CreateStep.PENDING_CONFIRM);
				session.setLastname(content);
				
				
				if (this.identityAlreadyExists(session)) {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					session.setCreateStep(CreateStep.NEED_TO_CHANGE);
				} else {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
				}
				
				session = em.merge(session);
				
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		case CHANGE_AVATARNAME: {
			if (content != null) {
				session.setCreateStep(CreateStep.PENDING_CONFIRM);
				session.setAvatarname(content);
				
				
				if (this.identityAlreadyExists(session)) {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					session.setCreateStep(CreateStep.NEED_TO_CHANGE);
				} else {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
				}
				
				session = em.merge(session);
				
			}
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		
		case CHANGE_AVATARPIC: {
			
			if (mm != null) {
				
				this.saveAvatarPicture(mm, session);
				
				if (session.getAvatarPic() != null) {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
						
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} else {
						session.setCreateStep(CreateStep.PENDING_CONFIRM);
						
					}
					session = em.merge(session);
				}
 				
			}
			
			
			this.createSendMessage(connectionId, threadId, session);
			break;
		}
		
		case CHANGE_BIRTHDATE: {
			
			if (content != null) {
				
				LocalDate birthDate = null;
				
				try {
					// parse date
					birthDate = LocalDate.from(df.parse(content));
					session.setBirthdate(birthDate);
					
					
					if (this.identityAlreadyExists(session)) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
						if (session.getAvatarPic() == null) {
							mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
						} else {
							MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
							mtProducer.sendMessage(mms);
							
						}
						session.setCreateStep(CreateStep.NEED_TO_CHANGE);
					} else {
						session.setCreateStep(CreateStep.PENDING_CONFIRM);
						
					}
					
					session = em.merge(session);
						
					
				} catch (Exception e) {
					logger.error("", e);
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("BIRTHDATE_ERROR")));
				}
				
			}
			
			this.createSendMessage(connectionId, threadId, session);
			
			
			break;
		}
		case CHANGE_PLACE_OF_BIRTH: {
			if (content != null) {

				session.setPlaceOfBirth(content);
				
				if (this.identityAlreadyExists(session)) {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_CREATE_ERROR_DUPLICATE_IDENTITY")));
					if (session.getAvatarPic() == null) {
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
					} else {
						MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
						mtProducer.sendMessage(mms);
						
					}
					session.setCreateStep(CreateStep.NEED_TO_CHANGE);
				} else {
					session.setCreateStep(CreateStep.PENDING_CONFIRM);
					
				}
				session = em.merge(session);
				
			} 
			this.createSendMessage(connectionId, threadId, session);
			
			break;
		}
		
		case PASSWORD: {
			
			if (content != null) {
				
				logger.info("createEntryPoint: password: " + content);
				
				Identity identity = session.getIdentity();
				identity.setPassword(DigestUtils.sha256Hex(content));
				identity.setProtectedTs(Instant.now());
				identity.setProtection(Protection.PASSWORD);
				em.persist(identity);
				
				
				
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_CONFIRM")));

				this.purgeSession(session);
				session.setType(SessionType.ISSUE);
				session.setIdentity(identity);
				session = em.merge(session);
				this.issueEntryPoint(connectionId, threadId, session, content);
				
			} else {
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_REQUEST")));
			}
			break;
		} case FACE_CAPTURE: {
			Token token = this.getToken(connectionId, TokenType.FACE_CAPTURE, session.getIdentity());
			
			mtProducer.sendMessage(generateFaceCaptureMediaMessage(connectionId, threadId, token));

			/*mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_CAPTURE_REQUEST.replaceFirst("URL", faceCaptureUrl.replaceFirst("TOKEN", token.getId().toString())
					.replaceFirst("REDIRDOMAIN", redirDomain)
					.replaceFirst("Q_DOMAIN", qRedirDomain)
					.replaceFirst("D_DOMAIN", dRedirDomain))));*/
			break;
		}
		case FINGERPRINT_CAPTURE: {
			Token token = this.getToken(connectionId, TokenType.FINGERPRINT_CAPTURE, session.getIdentity());
			
			break;
		}
		
		default: break;
	}
	}
	

	

	private MediaMessage buildSessionIdentityMediaMessage(UUID connectionId, UUID threadId, Session session) {
		MediaMessage mms = new MediaMessage();
		mms.setConnectionId(connectionId);
		mms.setThreadId(threadId);
		mms.setTimestamp(Instant.now());
		mms.setDescription(this.getSessionDataString(session));
		List<MediaItem> items = new ArrayList<MediaItem>();
		MediaItem item = new MediaItem();
		item.setMimeType(session.getAvatarMimeType());
		item.setUri(session.getAvatarURI());
		Ciphering c = new Ciphering();
		c.setAlgorithm(session.getAvatarPicCiphAlg());
		Parameters p = new Parameters();
		p.setIv(session.getAvatarPicCiphIv());
		p.setKey(session.getAvatarPicCiphKey());
		c.setParameters(p);
		item.setCiphering(c);
		item.setDescription(mms.getDescription());
		items.add(item);
		mms.setItems(items);
		return mms;
	}

	private Token getToken(UUID connectionId, TokenType type, Identity identity) {

		Query q = em.createNamedQuery("Token.findForConnection");
		q.setParameter("connectionId", connectionId);
		q.setParameter("type", type);
		Token token = (Token) q.getResultList().stream().findFirst().orElse(null);
		
		if (token == null) {
			token = new Token();
			token.setConnectionId(connectionId);
			token.setId(UUID.randomUUID());
			token.setType(type);
			token.setIdentity(identity);
			switch (type) {
			case FACE_CAPTURE: 
			case FINGERPRINT_CAPTURE: {
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
		} else {
			token.setConnectionId(connectionId);
			token.setType(type);
			token.setIdentity(identity);
			switch (type) {
			case FACE_CAPTURE: 
			case FINGERPRINT_CAPTURE: {
				token.setExpireTs(Instant.now().plus(Duration.ofSeconds(createTokenLifetimeSec)));
				break;
			}
			case FACE_VERIFICATION: 
			case FINGERPRINT_VERIFICATION: {
				token.setExpireTs(Instant.now().plus(Duration.ofSeconds(verifyTokenLifetimeSec)));
				break;
			}
			}
			token = em.merge(token);
		}
	
		return token;
				
		
	}

	private BaseMessage getWhichToChangeUserRequested(UUID connectionId, UUID threadId) {
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		
		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(getMessage("CHANGE_CLAIM_TITLE"));
		
		if (enableCitizenIdClaim) {
			MenuItem citizenId = new MenuItem();
			citizenId.setId(IdentityClaim.CITIZEN_ID.toString());
			citizenId.setText(IdentityClaim.CITIZEN_ID.getClaimLabel());
			menuItems.add(citizenId);
			
		}
		
		if (enableFirstnameClaim) {
			MenuItem firstname = new MenuItem();
			firstname.setId(IdentityClaim.FIRSTNAME.toString());
			firstname.setText(IdentityClaim.FIRSTNAME.getClaimLabel());
			menuItems.add(firstname);
		}
		if (enableLastnameClaim) {
			MenuItem lastname = new MenuItem();
			lastname.setId(IdentityClaim.LASTNAME.toString());
			lastname.setText(IdentityClaim.LASTNAME.getClaimLabel());
			menuItems.add(lastname);
		}
		
		if (enableAvatarnameClaim) {
			MenuItem avatarname = new MenuItem();
			avatarname.setId(IdentityClaim.AVATARNAME.toString());
			avatarname.setText(IdentityClaim.AVATARNAME.getClaimLabel());
			menuItems.add(avatarname);
		}
		
		if (enableAvatarpicClaim) {
			MenuItem avatarname = new MenuItem();
			avatarname.setId(IdentityClaim.AVATARPIC.toString());
			avatarname.setText(IdentityClaim.AVATARPIC.getClaimLabel());
			menuItems.add(avatarname);
		}
		if (enableBirthdateClaim) {
			MenuItem birthdate = new MenuItem();
			birthdate.setId(IdentityClaim.BIRTHDATE.toString());
			birthdate.setText(IdentityClaim.BIRTHDATE.getClaimLabel());
			menuItems.add(birthdate);
		}
		
		if (enableBirthplaceClaim) {
			MenuItem placeOfBirth = new MenuItem();
			placeOfBirth.setId(IdentityClaim.PLACE_OF_BIRTH.toString());
			placeOfBirth.setText(IdentityClaim.PLACE_OF_BIRTH.getClaimLabel());
			menuItems.add(placeOfBirth);
		}
		
		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		confirm.setMenuItems(menuItems);
		return confirm;
	}
	
	
	private BaseMessage getWhichToChangeNeeded(UUID connectionId, UUID threadId) {
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		
		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(getMessage("CONFLICTIVE_CLAIM_TITLE"));
		
		if (restoreCitizenidClaim) {
			MenuItem citizenId = new MenuItem();
			citizenId.setId(IdentityClaim.CITIZEN_ID.toString());
			citizenId.setText(IdentityClaim.CITIZEN_ID.getClaimLabel());
			menuItems.add(citizenId);
			
		}
		
		if (restoreFirstnameClaim) {
			MenuItem firstname = new MenuItem();
			firstname.setId(IdentityClaim.FIRSTNAME.toString());
			firstname.setText(IdentityClaim.FIRSTNAME.getClaimLabel());
			menuItems.add(firstname);
		}
		if (restoreLastnameClaim) {
			MenuItem lastname = new MenuItem();
			lastname.setId(IdentityClaim.LASTNAME.toString());
			lastname.setText(IdentityClaim.LASTNAME.getClaimLabel());
			menuItems.add(lastname);
		}
		
		if (restoreAvatarnameClaim) {
			MenuItem avatarname = new MenuItem();
			avatarname.setId(IdentityClaim.AVATARNAME.toString());
			avatarname.setText(IdentityClaim.AVATARNAME.getClaimLabel());
			menuItems.add(avatarname);
		}
		
		if (restoreBirthdateClaim) {
			MenuItem birthdate = new MenuItem();
			birthdate.setId(IdentityClaim.BIRTHDATE.toString());
			birthdate.setText(IdentityClaim.BIRTHDATE.getClaimLabel());
			menuItems.add(birthdate);
		}
		
		if (restoreBirthplaceClaim) {
			MenuItem placeOfBirth = new MenuItem();
			placeOfBirth.setId(IdentityClaim.PLACE_OF_BIRTH.toString());
			placeOfBirth.setText(IdentityClaim.PLACE_OF_BIRTH.getClaimLabel());
			menuItems.add(placeOfBirth);
		}
		
		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		confirm.setMenuItems(menuItems);
		return confirm;
	}
	
	private BaseMessage getConfirmData(UUID connectionId, UUID threadId) {

		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(getMessage("COMPLETE_IDENTITY_CONFIRM_TITLE"));

		MenuItem yes = new MenuItem();
		yes.setId(COMPLETE_IDENTITY_CONFIRM_YES_VALUE);
		yes.setText(getMessage("COMPLETE_IDENTITY_CONFIRM_YES"));
		
		MenuItem no = new MenuItem();
		no.setId(COMPLETE_IDENTITY_CONFIRM_NO_VALUE);
		no.setText(getMessage("COMPLETE_IDENTITY_CONFIRM_NO"));
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		
		confirm.setMenuItems(menuItems);


		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		
		return confirm;
	} 
	private void createSendMessage(UUID connectionId, UUID threadId, Session session) throws Exception {
		CreateStep step = session.getCreateStep();
		if (session.getCreateStep() == null) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME));
			if (WELCOME2.isPresent()) {
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME2.get()));
			}
			if (WELCOME3.isPresent()) {
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME3.get()));
			}
			// should not occur never
			if (step == null) {
				step = this.getNextCreateStep(null);
			}
		} 
		
		
		else switch (step) {
		
		case CITIZEN_ID:
		case CHANGE_CITIZEN_ID: 
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("CITIZEN_ID_REQUEST")));
			break;
		} 
		
		case FIRSTNAME:
		case CHANGE_FIRSTNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("FIRSTNAME_REQUEST")));
			break;
		} 
		case LASTNAME:
		case CHANGE_LASTNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("LASTNAME_REQUEST")));
			break;
		}
		case AVATARNAME:
		case CHANGE_AVATARNAME:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("AVATARNAME_REQUEST")));
			break;
		}
		case AVATARPIC:
		case CHANGE_AVATARPIC:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("AVATARPIC_REQUEST")));
			break;
		}
		case BIRTHDATE: 
		case CHANGE_BIRTHDATE: {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("BIRTHDATE_REQUEST")));
			break;
		}
		case PLACE_OF_BIRTH:
		case CHANGE_PLACE_OF_BIRTH:
		{
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PLACE_OF_BIRTH_REQUEST")));
			break;
		}
		
		
		case PENDING_CONFIRM: {
			if (session.getAvatarPic() == null) {
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, this.getSessionDataString(session)));
			} else {
				MediaMessage mms = this.buildSessionIdentityMediaMessage(connectionId, threadId, session);
				mtProducer.sendMessage(mms);
				
			}
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("CONFIRM_DATA_IMMUTABLE")));
			mtProducer.sendMessage(this.getConfirmData(connectionId, threadId));	
			break;
		}
		case PASSWORD: {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_REQUEST")));
			break;
		}
		
		case FACE_CAPTURE: {
			
			break;
		}
		
		case WANT_TO_CHANGE: {
			mtProducer.sendMessage(this.getWhichToChangeUserRequested(connectionId, threadId));
			break;
		}
		
		case NEED_TO_CHANGE: {
			mtProducer.sendMessage(this.getWhichToChangeNeeded(connectionId, threadId));
			break;
		}
		}
	}

	private Session issueCredentialAndSetEditMenu(Session session, Identity identity) throws Exception {

		if (identity.getAuthenticatedTs().plus(Duration.ofMinutes(authenticationValidForMinutes)).plus(Duration.ofSeconds(15)).compareTo(Instant.now())>=0) {
			logger.info("issueCredentialAndSetEditMenu: " + session.getFirstname());
			identity.setIssuedTs(Instant.now());
			identity.setRevokedTs(null);
			identity = em.merge(identity);
			this.sendCredential(session.getConnectionId(), identity);
			this.purgeSession(session);
			session.setIdentity(identity);
			session.setType(SessionType.EDIT);
			return em.merge(session);
		} else {
			identity.setAuthenticatedTs(null);
			identity = em.merge(identity);
		}
		return session;
	}

	public void issueEntryPoint(UUID connectionId, UUID threadId, Session session, String content) throws Exception {
		
		Identity identity = session.getIdentity();
		
		if ((identity.getAuthenticatedTs() != null)
				&& (identity.getAuthenticatedTs().plus(Duration.ofMinutes(authenticationValidForMinutes)).plus(Duration.ofSeconds(15)).compareTo(Instant.now())>=0)) {
			session = this.issueCredentialAndSetEditMenu(session, identity);
			session = em.merge(session);
			return;
		}
		
		if ((session.getIssueStep() == null) 
				
				) {
			
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("IDENTITY_LOCKED")));
			
			switch (identity.getProtection()) {
			
			case PASSWORD: {
				
				session.setIssueStep(IssueStep.PASSWORD_AUTH);
				session = em.merge(session);
				mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_VERIFICATION_REQUEST")));
				
				break;
			}
			case FACE: {
				session.setIssueStep(IssueStep.FACE_AUTH);
				session = em.merge(session);
				
				Token token = this.getToken(connectionId, TokenType.FACE_VERIFICATION, session.getIdentity());
				mtProducer.sendMessage(generateFaceVerificationMediaMessage(connectionId, threadId, token));
				/*mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_VERIFICATION_REQUEST.replaceFirst("URL", faceVerificationUrl.replaceFirst("TOKEN", token.getId().toString())
						.replaceFirst("REDIRDOMAIN", redirDomain)
						.replaceFirst("Q_DOMAIN", qRedirDomain)
						.replaceFirst("D_DOMAIN", dRedirDomain))));*/

				break;
			}
			case FINGERPRINTS: {
	
				session.setIssueStep(IssueStep.FINGERPRINT_AUTH);
				session = em.merge(session);
				
				Token token = this.getToken(connectionId, TokenType.FINGERPRINT_VERIFICATION, session.getIdentity());
				

				break;
			}
			}
		} else switch (session.getIssueStep()) {
			case PASSWORD_AUTH: {
				if (content != null) {
					logger.info("issueEntryPoint: password: " + content);
					String password = DigestUtils.sha256Hex(content);
					
					if ((identity.getPassword() != null) && (identity.getPassword().equals(password) && (identity.getConnectionId().equals(connectionId)))) {
						
						identity.setAuthenticatedTs(Instant.now());
						identity = em.merge(identity);
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("AUTHENTICATION_SUCCESSFULL")));
						
						session = this.issueCredentialAndSetEditMenu(session, identity);
						session = em.merge(session);
						
					} else {
						
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("INVALID_PASSWORD")));						
						mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_VERIFICATION_REQUEST")));

					}
				} else {
					mtProducer.sendMessage(TextMessage.build(connectionId, threadId, getMessage("PASSWORD_VERIFICATION_REQUEST")));
				}
				break;
			}
			case FACE_AUTH: {
				Token token = this.getToken(connectionId, TokenType.FACE_VERIFICATION, session.getIdentity());
				mtProducer.sendMessage(generateFaceVerificationMediaMessage(connectionId, threadId, token));
				/*mtProducer.sendMessage(TextMessage.build(connectionId, threadId, FACE_VERIFICATION_REQUEST.replaceFirst("URL", faceVerificationUrl.replaceFirst("TOKEN", token.getId().toString())
						.replaceFirst("REDIRDOMAIN", redirDomain)
						.replaceFirst("Q_DOMAIN", qRedirDomain)
						.replaceFirst("D_DOMAIN", dRedirDomain))));*/

				break;
			}
			
			case FINGERPRINT_AUTH: {
				Token token = this.getToken(connectionId, TokenType.FINGERPRINT_VERIFICATION, session.getIdentity());
				
				break;
			}
			
		}
		
	}

	
	
	private String getIdentityDataString(Identity identity) {
		StringBuffer data = new StringBuffer(1024);
		data.append(getMessage("IDENTITY_DATA_STR_HEADER")).append("\n");
		
		
		
		
		if (enableCitizenIdClaim) {
			data.append(IdentityClaim.CITIZEN_ID.getClaimLabel()).append(": ");
			
			if (identity.getCitizenId()!= null) {
				data.append(identity.getCitizenId()).append("\n");
			} else {
				data.append("<unset citizenId>").append("\n");
			}
		}
		
		if (enableFirstnameClaim) {
			data.append(IdentityClaim.FIRSTNAME.getClaimLabel()).append(": ");
			
			if (identity.getFirstname() != null) {
				data.append(identity.getFirstname()).append("\n");
			} else {
				data.append("<unset firstname>").append("\n");
			}
			
		}
		if (enableLastnameClaim) {
			data.append(IdentityClaim.LASTNAME.getClaimLabel()).append(": ");
			
			if (identity.getLastname() != null) {
				data.append(identity.getLastname()).append("\n");
			} else {
				data.append("<unset lastname>").append("\n");
			}
		}
		
		if (enableAvatarnameClaim) {
			data.append(IdentityClaim.AVATARNAME.getClaimLabel()).append(": ");
			
			if (identity.getAvatarname() != null) {
				data.append(identity.getAvatarname()).append("\n");
			} else {
				data.append("<unset avatarname>").append("\n");
			}
		}
		if (enableAvatarpicClaim) {
			data.append(IdentityClaim.AVATARPIC.getClaimLabel()).append(": ");
			
			if (identity.getAvatarPic() != null) {
				data.append(identity.getAvatarPic()).append("\n");
			} else {
				data.append("<unset avatarpic>").append("\n");
			}
		}
		if (enableBirthdateClaim) {
			data.append(IdentityClaim.BIRTHDATE.getClaimLabel()).append(": ");
			if (identity.getBirthdate() != null) {
				data.append(identity.getBirthdate()).append("\n");
			} else {
				data.append("<unset birthdate>").append("\n");
			}
		}
		
		if (enableBirthplaceClaim) {
			data.append(IdentityClaim.PLACE_OF_BIRTH.getClaimLabel()).append(": ");
			if (identity.getPlaceOfBirth() != null) {
				data.append(identity.getPlaceOfBirth()).append("\n");
			} else {
				data.append("<unset placeOfBirth>").append("\n");
			}
			
		}
		
		
		
		
		
		return data.toString();
		
	}
	
	
	private String getSessionDataString(Session session) {
		StringBuffer data = new StringBuffer(1024);
		data.append(getMessage("IDENTITY_DATA_STR_HEADER")).append("\n");
		
		if (enableCitizenIdClaim) {
			data.append(IdentityClaim.CITIZEN_ID.getClaimLabel()).append(": ");
			
			if (session.getCitizenId()!= null) {
				data.append(session.getCitizenId()).append("\n");
			} else {
				data.append("<unset citizenId>").append("\n");
			}
		}
		
		if (enableFirstnameClaim) {
			data.append(IdentityClaim.FIRSTNAME.getClaimLabel()).append(": ");
			
			if (session.getFirstname() != null) {
				data.append(session.getFirstname()).append("\n");
			} else {
				data.append("<unset firstname>").append("\n");
			}
			
		}
		if (enableLastnameClaim) {
			data.append(IdentityClaim.LASTNAME.getClaimLabel()).append(": ");
			
			if (session.getLastname() != null) {
				data.append(session.getLastname()).append("\n");
			} else {
				data.append("<unset lastname>").append("\n");
			}
		}
		
		if (enableAvatarnameClaim) {
			data.append(IdentityClaim.AVATARNAME.getClaimLabel()).append(": ");
			
			if (session.getAvatarname() != null) {
				data.append(session.getAvatarname()).append("\n");
			} else {
				data.append("<unset avatarname>").append("\n");
			}
		}
		if (enableAvatarpicClaim) {
			data.append(IdentityClaim.AVATARPIC.getClaimLabel()).append(": ");
			
			if (session.getAvatarPic() != null) {
				data.append(session.getAvatarPic()).append("\n");
			} else {
				data.append("<unset avatarpic>").append("\n");
			}
		}
		if (enableBirthdateClaim) {
			data.append(IdentityClaim.BIRTHDATE.getClaimLabel()).append(": ");
			if (session.getBirthdate() != null) {
				data.append(session.getBirthdate()).append("\n");
			} else {
				data.append("<unset birthdate>").append("\n");
			}
		}
		
		if (enableBirthplaceClaim) {
			data.append(IdentityClaim.PLACE_OF_BIRTH.getClaimLabel()).append(": ");
			if (session.getPlaceOfBirth() != null) {
				data.append(session.getPlaceOfBirth()).append("\n");
			} else {
				data.append("<unset placeOfBirth>").append("\n");
			}
			
		}
		
		return data.toString();
		
	}
	
	private CredentialType getCredentialType() {
		synchronized (lockObj) {
			if (type == null) {
				List<CredentialType> types = credentialTypeResource.getAllCredentialTypes();
				
				if ( (types == null) || (types.size()==0) ) {
					
					CredentialType newType = new CredentialType();
					newType.setName(defName);
					newType.setVersion("1.0");
					
					List<String> attributes = new ArrayList<String>();
					attributes.add("id");
					if (enableCitizenIdClaim) attributes.add("citizenId");
					if (enableFirstnameClaim) attributes.add("firstname");
					if (enableLastnameClaim) attributes.add("lastname");
					if (enableAvatarnameClaim) attributes.add("avatarName");
					if (enableAvatarpicClaim) attributes.add("avatarPic");
					if (enableBirthdateClaim) attributes.add("birthdate");
					if (enableBirthplaceClaim) attributes.add("placeOfBirth");
					if (enablePhotoClaim) attributes.add("photo");
					attributes.add("citizenSince");
					attributes.add("issued");
					
					newType.setAttributes(attributes);
					try {
						logger.info("getCredentialType: create: " + JsonUtil.serialize(newType, false));
					} catch (JsonProcessingException e) {
						
					}
					
					credentialTypeResource.createCredentialType(newType);
					
					types = credentialTypeResource.getAllCredentialTypes();
				}
				type = types.iterator().next();
			}
		}
		return type;
	}

	
	private void sendCredential(UUID connectionId, Identity id) throws Exception {
		
		CredentialIssuanceMessage cred = new CredentialIssuanceMessage();
		cred.setConnectionId(connectionId);
		cred.setCredentialDefinitionId(getCredentialType().getId());
		
		if (id == null) return;
		
		List<Claim> claims = new ArrayList<Claim>();
		
		Claim idId = new Claim();
		idId.setName("id");
		idId.setValue(id.getId().toString());
		claims.add(idId);
		
		if (enableCitizenIdClaim) {
			Claim citizenId = new Claim();
			citizenId.setName("citizenId");
			citizenId.setValue(id.getCitizenId().toString());
			claims.add(citizenId);
		}
		if (enableFirstnameClaim) {
			Claim firstname = new Claim();
			firstname.setName("firstname");
			firstname.setValue(id.getFirstname());
			claims.add(firstname);
		}
		if (enableLastnameClaim) {
			Claim lastname = new Claim();
			lastname.setName("lastname");
			lastname.setValue(id.getLastname());
			claims.add(lastname);
		}
		if (enableAvatarnameClaim) {
			Claim avatarname = new Claim();
			avatarname.setName("avatarName");
			avatarname.setValue(id.getAvatarname());
			claims.add(avatarname);
		}
		if (enableAvatarpicClaim) {
			
			
			
			
			UUID mediaId = id.getAvatarPic();
			String mimeType = id.getAvatarMimeType();
			
			if (mediaId == null) {
				logger.error("sendCredential: no media defined for id " + id.getId());
				throw new NoMediaException();
			}
			
			byte[] imageBytes = mediaResource.render(mediaId);
			
			
			
			if (imageBytes == null) {
				logger.error("sendCredential: datastore returned null value for mediaId " + mediaId + " id " + id.getId());
				throw new NoMediaException();
			}
			if (mimeType == null) {
				mimeType = "image/jpeg";
			}
			
			logger.info("sendCredential: imageBytes: " + imageBytes.length + " " + id.getAvatarPicCiphIv() + " " + id.getAvatarPicCiphKey());
			
			byte[] decrypted = Aes256cbc.decrypt(id.getAvatarPicCiphKey(), id.getAvatarPicCiphIv(), imageBytes);
			logger.info("sendCredential: decrypted: " + decrypted.length);
			
			Claim image = new Claim();
			image.setName("avatarPic");
			String encPhoto = "data:" + mimeType + ";base64," + Base64.encodeBytes(decrypted);
			
			image.setValue(encPhoto);
			
			if (debug) {
				logger.info("sendCredential: avatarpic: " + encPhoto);
				logger.info("sendCredential: avatarpic: " + JsonUtil.serialize(image, false));
				logger.info("sendCredential: avatarpic: encPhoto.length: " + encPhoto.length());
				
			}
			claims.add(image);
		}
		if (enableBirthdateClaim) {
			Claim birthdate = new Claim();
			birthdate.setName("birthdate");
			birthdate.setValue(id.getBirthdate().toString());
			claims.add(birthdate);
		}
		if (enableBirthplaceClaim) {
			Claim placeOfBirth = new Claim();
			placeOfBirth.setName("placeOfBirth");
			placeOfBirth.setValue(id.getPlaceOfBirth());
			
			claims.add(placeOfBirth);
		}
		
		
		
		
		if (enablePhotoClaim) {
			
			Query q = this.em.createNamedQuery("Media.find");
			q.setParameter("identity", id);
			q.setParameter("type", MediaType.FACE);
			List<UUID> faceMedias = q.getResultList();
			
			if (faceMedias.size() <1) {
				logger.error("sendCredential: faceMedias.size() " + faceMedias.size() + " id " + id.getId());
				throw new NoMediaException();
			}
			UUID mediaId = faceMedias.iterator().next();
			byte[] imageBytes = mediaResource.render(mediaId);
			
			if (imageBytes == null) {
				logger.error("sendCredential: datastore returned null value for mediaId " + mediaId + " id " + id.getId());
				throw new NoMediaException();
			}
			
			String mimeType = em.find(Media.class, mediaId).getMimeType();
			if (mimeType == null) {
				mimeType = "image/jpeg";
			}
			
			Claim image = new Claim();
			image.setName("photo");
			String encPhoto = "data:" + mimeType + ";base64," + Base64.encodeBytes(imageBytes);image.setValue(encPhoto);
			
			claims.add(image);
			
			
			if (debug) {
				logger.info("sendCredential: photo: " + encPhoto);
				logger.info("sendCredential: photo: " + JsonUtil.serialize(image, false));
			}
		}
		
		
		
		
		
		Claim citizenSince = new Claim();
		citizenSince.setName("citizenSince");
		citizenSince.setValue(id.getCitizenSinceTs().toString());
		
		Claim issued = new Claim();
		issued.setName("issued");
		issued.setValue(id.getIssuedTs().toString());
		
		
		claims.add(citizenSince);
		claims.add(issued);
		cred.setClaims(claims);
		try {
			logger.info("sendCredential: " + JsonUtil.serialize(cred, false));
		} catch (JsonProcessingException e) {
		}
		mtProducer.sendMessage(cred);
	}
	
	public void newConnection(UUID connectionId) throws Exception {
		UUID threadId = UUID.randomUUID();
		mtProducer.sendMessage(TextMessage.build(connectionId,threadId , WELCOME));
		if (WELCOME2.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME2.get()));
		}
		if (WELCOME3.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME3.get()));
		}
		mtProducer.sendMessage(this.getRootMenu(connectionId, null, null));
		
		//entryPointCreate(connectionId, null, null);
	}
	
	private void purgeSession(Session session) {
		if (session != null) {
			session.setBirthdate(null);
			session.setCreateStep(null);
			session.setFirstname(null);
			session.setIdentity(null);
			session.setLastname(null);
			session.setPlaceOfBirth(null);
			session.setRestoreStep(null);
			session.setType(null);
		}
		
	}
	private Session getSession(UUID connectionId) {
		return em.find(Session.class, connectionId);
	}
	private Session createSession(Session session, UUID connectionId) {
		if (session == null) {
			session = new Session();
			session.setConnectionId(connectionId);
			em.persist(session);
		} else {
			session.setBirthdate(null);
			session.setCreateStep(null);
			session.setFirstname(null);
			session.setIdentity(null);
			session.setLastname(null);
			session.setPlaceOfBirth(null);
			session.setRestoreStep(null);
			session.setType(null);
		}
		return session;
	}

	
	@Transactional
	public void notifySuccess(Token token) throws Exception {
		
		Identity identity = token.getIdentity();
		Session session = getSession(token.getConnectionId());
		
		try {
			logger.info("userInput: session: " + JsonUtil.serialize(session, false));
		} catch (JsonProcessingException e) {
			
		}
		switch (token.getType()) {
		case FACE_CAPTURE: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.CREATE)) 
						&& (identity.getProtection().equals(Protection.FACE))
						&& (session.getCreateStep().equals(CreateStep.FACE_CAPTURE))
						&& (identity.getProtectedTs() == null)
						) {
					
					identity.setProtectedTs(Instant.now());
					
					identity = em.merge(identity);
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FACE_CAPTURE_SUCCESSFULL")));
					
					this.purgeSession(session);
					session.setType(SessionType.ISSUE);
					session.setIdentity(identity);
					session = em.merge(session);
					this.issueEntryPoint(session.getConnectionId(), null, session, null);
					
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
			
		case FINGERPRINT_CAPTURE: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.CREATE)) 
						&& (identity.getProtection().equals(Protection.FINGERPRINTS))
						&& (session.getCreateStep().equals(CreateStep.FINGERPRINT_CAPTURE))
						&& (identity.getProtectedTs() == null)
						) {
					
					identity.setProtectedTs(Instant.now());
					
					identity = em.merge(identity);
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FINGERPRINT_CAPTURE_SUCCESSFULL")));
					
					this.purgeSession(session);
					session.setType(SessionType.ISSUE);
					session.setIdentity(identity);
					session = em.merge(session);
					this.issueEntryPoint(session.getConnectionId(), null, session, null);
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		case FACE_VERIFICATION: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.ISSUE) 
								|| session.getType().equals(SessionType.RESTORE)) 
						&& (identity.getProtection().equals(Protection.FACE))
						&& (identity.getProtectedTs() != null)
						) {
					
					identity.setAuthenticatedTs(Instant.now());
					identity.setConnectionId(session.getConnectionId());
					
					identity = em.merge(identity);
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("AUTHENTICATION_SUCCESSFULL")));
					
					session = this.issueCredentialAndSetEditMenu(session, identity);
					
					mtProducer.sendMessage(this.getRootMenu(session.getConnectionId(), session, identity));
					
				
				} else {
					logger.info("notifySuccess: session: " + JsonUtil.serialize(session, false));
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		case FINGERPRINT_VERIFICATION: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.ISSUE) || session.getType().equals(SessionType.RESTORE)) 
						&& (identity.getProtection().equals(Protection.FINGERPRINTS))
						&& (identity.getProtectedTs() != null)
						) {
					
					identity.setAuthenticatedTs(Instant.now());
					identity.setConnectionId(session.getConnectionId());
					identity = em.merge(identity);
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("AUTHENTICATION_SUCCESSFULL")));
					
					session = this.issueCredentialAndSetEditMenu(session, identity);
					
					mtProducer.sendMessage(this.getRootMenu(session.getConnectionId(), session, identity));
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		}
		
		
		
	}

	@Transactional
	public void notifyFailure(Token token) throws Exception {
		Identity identity = token.getIdentity();
		Session session = getSession(token.getConnectionId());
		try {
			logger.info("userInput: session: " + JsonUtil.serialize(session, false));
		} catch (JsonProcessingException e) {
			
		}
		switch (token.getType()) {
		case FACE_CAPTURE: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.CREATE)) 
						&& (identity.getProtection().equals(Protection.FACE))
						&& (session.getCreateStep().equals(CreateStep.FACE_CAPTURE))
						&& (identity.getProtectedTs() == null)
						) {
					
					
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FACE_CAPTURE_ERROR")));
					
					this.createEntryPoint(session.getConnectionId(), null, session, null, null);
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
			
		case FINGERPRINT_CAPTURE: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.CREATE)) 
						&& (identity.getProtection().equals(Protection.FINGERPRINTS))
						&& (session.getCreateStep().equals(CreateStep.FINGERPRINT_CAPTURE))
						&& (identity.getProtectedTs() == null)
						) {
					

					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FINGERPRINT_CAPTURE_ERROR")));
					
					this.createEntryPoint(session.getConnectionId(), null, session, null, null);
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		case FACE_VERIFICATION: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.ISSUE) || session.getType().equals(SessionType.RESTORE)) 
						&& (identity.getProtection().equals(Protection.FACE))
						&& (identity.getProtectedTs() != null)
						) {
					
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FACE_AUTHENTICATION_ERROR")));
					
					if (session.getType().equals(SessionType.ISSUE)) {
						this.issueEntryPoint(session.getConnectionId(), null, session, null);
					} else {
						this.restoreEntryPoint(session.getConnectionId(), null, session, null);
					}
					
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		case FINGERPRINT_VERIFICATION: {
			if (session != null) {
				if ((session.getType() != null) 
						&& (session.getType().equals(SessionType.ISSUE) || session.getType().equals(SessionType.RESTORE)) 
						&& (identity.getProtection().equals(Protection.FINGERPRINTS))
						&& (identity.getProtectedTs() != null)
						) {
					
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("FINGERPRINT_AUTHENTICATION_ERROR")));
					
					if (session.getType().equals(SessionType.ISSUE)) {
						this.issueEntryPoint(session.getConnectionId(), null, session, null);
					} else {
						this.restoreEntryPoint(session.getConnectionId(), null, session, null);
					}
					
				
				} else {
					throw new TokenException();
				}
			} else {
				throw new TokenException();
			}
			break;
		}
		
		}
		
		
		
	}
	/*
	private static String MEDIA_NO_ATTACHMENT_ERROR = "Received message does not include any attachment.";
	private static String MEDIA_SIZE_ERROR = "Received media is too big. Make sure it is smaller than 5MB.";
	private static String MEDIA_TYPE_ERROR = "Received media is not an image. Accepted: image/jpeg, image/png, image/svg+xml";
	private static String MEDIA_URI_ERROR = "Received media has no URI";
	private static String MEDIA_SAVE_ERROR = "Cannot save Avatar";
	*/
	
	private void saveAvatarPicture(MediaMessage mm, Session session) throws Exception{
		UUID uuid = null;
		String mediaType = null;
		List<MediaItem> items = mm.getItems();
		
		if (items.size() == 0) {
			logger.info("incomingAvatarPicture: no items");
			mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_NO_ATTACHMENT_ERROR")));
			session.setAvatarPic(null);
			session.setAvatarPicCiphAlg(null);
			session.setAvatarPicCiphIv(null);
			session.setAvatarPicCiphKey(null);
			session.setAvatarMimeType(null);
			session.setAvatarURI(null);
			return;
		}
		
		MediaItem item = items.iterator().next();
		
			if (item.getByteCount() < 5000000) {
				
				mediaType = item.getMimeType();
				
				if ((mediaType != null) && (mediaType.length()>0)) {
					mediaType = mediaType.toLowerCase().strip();
					
					if ( 
							(mediaType.equals("image/svg+xml"))
						|| 	(mediaType.equals("image/jpg"))
						|| 	(mediaType.equals("image/jpeg"))
						||  (mediaType.equals("image/png"))
							
					) {
					
						Ciphering c = item.getCiphering();
						Parameters p = c.getParameters();
						//logger.info("saveAvatarPicture: ciphering: " + c.getAlgorithm() + " Key " + p.getKey() + " Iv " + p.getIv());
												
						if (item.getUri() != null) {
							
							try {
								byte[] encrypted = this.getMedia(item.getUri());
								byte[] reencrypted = encrypted;
								
								if (!(mediaType.equals("image/svg+xml"))) {
									byte[] decrypted = Aes256cbc.decrypt(p.getKey(), p.getIv(), encrypted);
									InputStream inputStream = new ByteArrayInputStream(decrypted);
									BufferedImage image = ImageIO.read(inputStream);
									BufferedImage outputImage = image;
									
									int currentWidth = image.getWidth();
									int currentHeight = image.getHeight();
									
									
									
									if ((currentWidth >avatarMaxDim) || (currentHeight > avatarMaxDim)) {
										
										float width = (float)image.getWidth();
										float height = (float)image.getHeight();
										
										int newImageWidth = currentWidth;
										int newImageHeight = currentHeight;
										
										
										if (width >= height) {
											float newWidth = avatarMaxDim;
											float newHeight = height * (newWidth/width);
											newImageWidth = (int) newWidth;
											newImageHeight = (int)newHeight;
											
											
										} else {
											float newHeight = avatarMaxDim;
											float newWidth = width * (newHeight / height);
											newImageWidth = (int) newWidth;
											newImageHeight = (int)newHeight;
										}
										Image resultingImage = image.getScaledInstance(newImageWidth, newImageHeight, Image.SCALE_SMOOTH);
									    outputImage = new BufferedImage(newImageWidth, newImageHeight, BufferedImage.TYPE_INT_RGB);
									    outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
										
									    logger.info("incomingAvatarPicture: old size: " + currentWidth + "x" + currentHeight);

									    logger.info("incomingAvatarPicture: new size: " + newImageWidth + "x" + newImageHeight);
									}
									 
									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									if (mediaType.equals("image/jpg")) {
										ImageIO.write(outputImage, "jpg", baos);
									} else if (mediaType.equals("image/jpeg")) {
										ImageIO.write(outputImage, "jpg", baos);
									} else if (mediaType.equals("image/png")) {
										ImageIO.write(outputImage, "png", baos);
									} 
									baos.close();
							        byte[] bytes = baos.toByteArray();
							        logger.info("saveAvatarPicture: reencrypted: " + c.getAlgorithm() + " Key " + p.getKey() + " Iv " + p.getIv() + " size: " + bytes.length);
									
									reencrypted = Aes256cbc.encrypt(p.getKey(), p.getIv(), bytes);
									
								}
								
								// properly deciphered
								
								uuid = UUID.randomUUID();
								mediaResource.createOrUpdate(uuid, 1, null);
								File file = new File(System.getProperty("java.io.tmpdir") + "/" + uuid);
								
								FileOutputStream fos = new FileOutputStream(file);
								fos.write(reencrypted);
								fos.flush();
								fos.close();
								
								Resource r = new Resource();
								r.chunk = new FileInputStream(file);
								mediaResource.uploadChunk(uuid, 0, null, r);
								
								file.delete();
								
								session.setAvatarMimeType(mediaType);
								session.setAvatarPic(uuid);
								session.setAvatarPicCiphAlg(c.getAlgorithm());
								session.setAvatarPicCiphIv(p.getIv());
								session.setAvatarPicCiphKey(p.getKey());
								session.setAvatarURI(item.getUri());
								
							} catch (Exception e) {
								logger.error("incomingAvatarPicture", e);
								logger.info("incomingAvatarPicture: could not save avatar");
								mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_SAVE_ERROR")));
								session.setAvatarPic(null);
								session.setAvatarPicCiphAlg(null);
								session.setAvatarPicCiphIv(null);
								session.setAvatarPicCiphKey(null);
								session.setAvatarMimeType(null);
								session.setAvatarURI(null);
								return;
							}
						} else {
							
							logger.info("incomingAvatarPicture: no uri");
							mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_URI_ERROR")));
							session.setAvatarPic(null);
							session.setAvatarPicCiphAlg(null);
							session.setAvatarPicCiphIv(null);
							session.setAvatarPicCiphKey(null);
							session.setAvatarMimeType(null);
							session.setAvatarURI(null);
							return;
							
							
						}
						
					} else {
						logger.info("incomingAvatarPicture: invalid type: " + mediaType);
						mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_TYPE_ERROR")));
						session.setAvatarPic(null);
						session.setAvatarPicCiphAlg(null);
						session.setAvatarPicCiphIv(null);
						session.setAvatarPicCiphKey(null);
						session.setAvatarMimeType(null);
						session.setAvatarURI(null);
						return;
					} 
				} else {
					logger.info("incomingAvatarPicture: invalid type: " + mediaType);
					mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_TYPE_ERROR")));
					session.setAvatarPic(null);
					session.setAvatarPicCiphAlg(null);
					session.setAvatarPicCiphIv(null);
					session.setAvatarPicCiphKey(null);
					session.setAvatarMimeType(null);
					session.setAvatarURI(null);
					return;
				}
				
			} else {
				// too big too big ;-)
				logger.info("incomingAvatarPicture: no items");
				mtProducer.sendMessage(TextMessage.build(session.getConnectionId(), null, getMessage("MEDIA_SIZE_ERROR")));
				session.setAvatarPic(null);
				session.setAvatarPicCiphAlg(null);
				session.setAvatarPicCiphIv(null);
				session.setAvatarPicCiphKey(null);
				session.setAvatarMimeType(null);
				session.setAvatarURI(null);
				return;
			}
			
		
	}
	
	
	private byte[] getMedia(String uri) throws IOException, ClientProtocolException {
		HttpClient httpclient = HttpClientBuilder.create().build();
	    HttpGet httpget = new HttpGet(uri);
	    HttpResponse response = httpclient.execute(httpget);
	    HttpEntity entity = response.getEntity();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	    entity.writeTo(baos);
	    return baos.toByteArray();
	}
	
}
