package io.twentysixty.demos.auth.svc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;
import org.jgroups.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.demos.auth.model.Session;
import io.twentysixty.demos.auth.registry.jms.MtProducer;
import io.twentysixty.demos.auth.res.c.MediaResource;
import io.twentysixty.demos.auth.res.c.Resource;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.Claim;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.IdentityProofRequestMessage;
import io.twentysixty.sa.client.model.message.IdentityProofSubmitMessage;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaItem;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.RequestedProofItem;
import io.twentysixty.sa.client.model.message.SubmitProofItem;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.CredentialTypeResource;



@ApplicationScoped
public class Service {

	private static Logger logger = Logger.getLogger(Service.class);

	@Inject EntityManager em;
	
	@RestClient
	@Inject MediaResource mediaResource;
	
	
	@Inject MtProducer mtProducer;
	
	@RestClient
	@Inject CredentialTypeResource credentialTypeResource;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.debug")
	Boolean debug;
	
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.credential_issuer")
	String credentialIssuer;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.credential_issuer.avatar")
	String invitationImageUrl;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.credential_issuer.label")
	String invitationLabel;
	
	
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.id_credential_def")
	String credDef;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.welcome")
	String WELCOME;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.welcome2")
	Optional<String> WELCOME2;

	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.welcome3")
	Optional<String> WELCOME3;

	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.auth_success")
	Optional<String> AUTH_SUCCESS;

	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.nocred")
	String NO_CRED_MSG;

	@ConfigProperty(name = "io.twentysixty.demos.auth.request.citizenid")
	Boolean requestCitizenId;

	
	@ConfigProperty(name = "io.twentysixty.demos.auth.request.firstname")
	Boolean requestFirstname;

	@ConfigProperty(name = "io.twentysixty.demos.auth.request.lastname")
	Boolean requestLastname;

	@ConfigProperty(name = "io.twentysixty.demos.auth.request.photo")
	Boolean requestPhoto;

	@ConfigProperty(name = "io.twentysixty.demos.auth.request.avatarname")
	Boolean requestAvatarname;

		
	@ConfigProperty(name = "io.twentysixty.demos.auth.language")
	Optional<String> language;

	@ConfigProperty(name = "io.twentysixty.demos.auth.vision.face.verification.url")
	String faceVerificationUrl;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.vision.redirdomain")
	Optional<String> redirDomain;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.vision.redirdomain.q")
	Optional<String> qRedirDomain;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.vision.redirdomain.d")
	Optional<String> dRedirDomain;
	
	private static String CMD_ROOT_MENU_AUTHENTICATE = "/auth";
	private static String CMD_ROOT_MENU_NO_CRED = "/nocred";
	private static String CMD_ROOT_MENU_OPTION1 = "/option1";
	private static String CMD_ROOT_MENU_LOGOUT = "/logout";
	
	
	
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.root.menu.title")
	String ROOT_MENU_TITLE;
	
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.root.menu.option1")
	String ROOT_MENU_OPTION1;
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.root.menu.no_cred")
	Optional<String> ROOT_MENU_NO_CRED;
	
	
	
	@ConfigProperty(name = "io.twentysixty.demos.auth.messages.option1")
	String OPTION1_MSG;
	
	
	
	
	
	
	//private static HashMap<UUID, SessionData> sessions = new HashMap<UUID, SessionData>();
	private static CredentialType type = null;
	private static Object lockObj = new Object();
	
	
	
	public void newConnection(UUID connectionId) throws Exception {
		UUID threadId = UUID.randomUUID();
		mtProducer.sendMessage(TextMessage.build(connectionId,threadId , WELCOME));
		if (WELCOME2.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME2.get()));
		}
		if (WELCOME3.isPresent()) {
			mtProducer.sendMessage(TextMessage.build(connectionId, threadId, WELCOME3.get()));
		}
		
		
		mtProducer.sendMessage(this.getRootMenu(connectionId, null));
		
		mtProducer.sendMessage(this.getIdentityCredentialRequest(connectionId, null));
		//entryPointCreate(connectionId, null, null);
	}
	

	
	private BaseMessage getIdentityCredentialRequest(UUID connectionId, UUID threadId) {
		IdentityProofRequestMessage ip = new IdentityProofRequestMessage();
		ip.setConnectionId(connectionId);
		ip.setThreadId(threadId);
		
		RequestedProofItem id = new RequestedProofItem();
		id.setCredentialDefinitionId(credDef);
		id.setType("verifiable-credential");
		List<String> attributes = new ArrayList<String>();
		if (requestCitizenId) attributes.add("citizenId");
		if (requestFirstname) attributes.add("firstname");
		if (requestLastname) attributes.add("lastname");
		if (requestPhoto) attributes.add("photo");
		if (requestAvatarname) attributes.add("avatarName");
		attributes.add("issued");
		id.setAttributes(attributes);
		
		List<RequestedProofItem> rpi = new ArrayList<RequestedProofItem>();
		rpi.add(id);
		
		ip.setRequestedProofItems(rpi);
	
		
		try {
			logger.info("getCredentialRequest: claim: " + JsonUtil.serialize(ip, false));
		} catch (Exception e) {
			
		}
		return ip;
	}
	
	private Session getSession(UUID connectionId) {
		Session session = em.find(Session.class, connectionId);
		if (session == null) {
			session = new Session();
			session.setConnectionId(connectionId);
			em.persist(session);
			
		}
		
		return session;
	}
	
	
	
	Pair<String, byte[]> getImage(String image) {
		String mimeType = null;
		byte[] imageBytes = null;
		
		String[] separated =  image.split(";");
		if (separated.length>1) {
			String[] mimeTypeData = separated[0].split(":");
			String[] imageData = separated[1].split(",");
			
			if (mimeTypeData.length>1) {
				mimeType = mimeTypeData[1];
			}
			if (imageData.length>1) {
				String base64Image = imageData[1];
				if (base64Image != null) {
					try {
						imageBytes = Base64.decode(base64Image);
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			}
			
		}
		
		if (mimeType == null) return null;
		if (imageBytes == null) return null;
		
		return Pair.create(mimeType, imageBytes);
		
	}
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
	private BaseMessage generateFaceVerificationMediaMessage(UUID connectionId, UUID threadId, String token) {
		String url = faceVerificationUrl.replaceFirst("TOKEN", token);
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
	
	
	@Transactional
	public void userInput(BaseMessage message) throws Exception {
		
		Session session = this.getSession(message.getConnectionId());
		
		
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
		} else if ((message instanceof IdentityProofSubmitMessage)) {
			if (session.getAuthTs() == null) {
				try {
					logger.info("userInput: claim: " + JsonUtil.serialize(message, false));
				} catch (JsonProcessingException e) {
					
				}
				boolean sentVerifLink = false;
				IdentityProofSubmitMessage ipm = (IdentityProofSubmitMessage) message;
				
				if (ipm.getSubmittedProofItems().size()>0) {
					
					SubmitProofItem sp = ipm.getSubmittedProofItems().iterator().next();
					
					if ((sp.getVerified() != null) && (sp.getVerified())) {
						if (sp.getClaims().size()>0) {
							
							String citizenId = null;
							String firstname = null;
							String lastname = null;
							String photo = null;
							String avatarName = null;
							
							for (Claim c: sp.getClaims()) {
								if (c.getName().equals("citizenId")) {
									citizenId = c.getValue();
								} else if (c.getName().equals("firstname")) {
									firstname = c.getValue();
								} else if (c.getName().equals("lastname")) {
									lastname = c.getValue();
								} else if (c.getName().equals("photo")) {
									photo = c.getValue();
									logger.info("userInput: photo: " + photo);
								} else if (c.getName().equals("avatarName")) {
									avatarName = c.getValue();
								} 
							}
							session.setCitizenId(citizenId);
							session.setFirstname(firstname);
							session.setLastname(lastname);
							session.setAvatarName(avatarName);
							
							if (photo != null) {
								Pair<String, byte[]> imageData = getImage(photo);
								if (imageData != null) {
									UUID mediaUUID = UUID.randomUUID();
									mediaResource.createOrUpdate(mediaUUID, 1, mediaUUID.toString());
									
									
									File file = new File(System.getProperty("java.io.tmpdir") + "/" + mediaUUID);
									
									FileOutputStream fos = new FileOutputStream(file);
									fos.write(imageData.getRight());
									fos.flush();
									fos.close();
									
									Resource r = new Resource();
									r.chunk = new FileInputStream(file);
									mediaResource.uploadChunk(mediaUUID, 0, mediaUUID.toString(), r);
									
									file.delete();
									session.setPhoto(mediaUUID);
									session.setToken(UUID.randomUUID());
									em.merge(session);
									
									mtProducer.sendMessage(generateFaceVerificationMediaMessage(message.getConnectionId(), message.getThreadId(), session.getToken().toString()));
									
									sentVerifLink = true;
								}
							}
							
							
						}
					}
					} else {
						// user do not have the required credential, send invitation link
						
						mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , NO_CRED_MSG));
						mtProducer.sendMessage(this.getInvitationMessage(message.getConnectionId(), message.getThreadId()));
						
					}
					
					
				if (!sentVerifLink) {
					
					notifySuccess(session.getConnectionId());
					
					//mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("CREDENTIAL_ERROR")));

				}
			}
			
		}
		if (content != null) {
			if (content.equals(CMD_ROOT_MENU_AUTHENTICATE.toString())) {
				mtProducer.sendMessage(this.getIdentityCredentialRequest(message.getConnectionId(), message.getThreadId()));
			} else if (content.equals(CMD_ROOT_MENU_OPTION1.toString())) {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , OPTION1_MSG));

			} else if (content.equals(CMD_ROOT_MENU_NO_CRED.toString())) {
				
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , NO_CRED_MSG));
				mtProducer.sendMessage(this.getInvitationMessage(message.getConnectionId(), message.getThreadId()));

			} else if (content.equals(CMD_ROOT_MENU_LOGOUT.toString())) {
				if (session != null) {
					session.setAuthTs(null);
					session = em.merge(session);
				}
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("UNAUTHENTICATED")));

			} else {
				mtProducer.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId() , this.getMessage("ERROR")));
			}
		}
		mtProducer.sendMessage(this.getRootMenu(message.getConnectionId(), session));
	}
	
	
	private BaseMessage getInvitationMessage(UUID connectionId, UUID threadId) {
		InvitationMessage invitation = new InvitationMessage();
		invitation.setConnectionId(connectionId);
		invitation.setThreadId(threadId);
		invitation.setImageUrl(invitationImageUrl);
		invitation.setDid(credentialIssuer);
		invitation.setLabel(invitationLabel);
		return invitation;
	}

	@Transactional
	public void notifySuccess(UUID connectionId) {
		Session session = em.find(Session.class, connectionId);
		if (session != null) {
			try {
				session.setAuthTs(Instant.now());
				session = em.merge(session);
				if (AUTH_SUCCESS.isPresent()) {
					mtProducer.sendMessage(TextMessage.build(connectionId, null , AUTH_SUCCESS.get()));
				} else {
					mtProducer.sendMessage(TextMessage.build(connectionId, null , this.getMessage("AUTHENTICATION_SUCCESS")));
				}
				
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		try {
			mtProducer.sendMessage(this.getRootMenu(connectionId, session));
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	public void notifyFailure(UUID connectionId) {
		Session session = em.find(Session.class, connectionId);
		if (session != null) {
			try {
				mtProducer.sendMessage(TextMessage.build(connectionId, null , this.getMessage("AUTHENTICATION_ERROR")));
				
				mtProducer.sendMessage(generateFaceVerificationMediaMessage(connectionId, null, session.getToken().toString()));
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
	
	
	
	public BaseMessage getRootMenu(UUID connectionId, Session session) {
		
		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(ROOT_MENU_TITLE);
		
		
		List<ContextualMenuItem> options = new ArrayList<ContextualMenuItem>();
		
		
		if ((session == null) || (session.getAuthTs() == null) ){
			menu.setDescription(getMessage("ROOT_MENU_DEFAULT_DESCRIPTION"));
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_AUTHENTICATE, getMessage("ROOT_MENU_AUTHENTICATE"), null));
			if (ROOT_MENU_NO_CRED.isPresent()) {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, ROOT_MENU_NO_CRED.get(), null));
			} else {
				options.add(ContextualMenuItem.build(CMD_ROOT_MENU_NO_CRED, getMessage("ROOT_MENU_NO_CRED"), null));
			}
			
			
		} else {
			if ((session.getFirstname() != null) && (session.getLastname() != null)) {
				menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION").replaceAll("NAME", session.getFirstname() + " " + session.getLastname()));
				
			} else if (session.getAvatarName() != null){
				menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION").replaceAll("NAME", session.getAvatarName()));
				
			} else if (session.getCitizenId() != null) {
				menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION").replaceAll("NAME", session.getCitizenId()));
				
			} else {
				menu.setDescription(getMessage("ROOT_MENU_AUTHENTICATED_DESCRIPTION").replaceAll("NAME", ""));
				
			}
			
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_OPTION1, ROOT_MENU_OPTION1, null));
			options.add(ContextualMenuItem.build(CMD_ROOT_MENU_LOGOUT, this.getMessage("ROOT_MENU_LOGOUT"), null));
			
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
}
