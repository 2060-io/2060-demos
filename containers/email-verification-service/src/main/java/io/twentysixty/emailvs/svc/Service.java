package io.twentysixty.emailvs.svc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.emailvs.enums.EmailType;
import io.twentysixty.emailvs.enums.RevocationState;
import io.twentysixty.emailvs.enums.VerificationStep;
import io.twentysixty.emailvs.model.Verified;
import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.Claim;
import io.twentysixty.sa.client.model.message.ContextualMenuItem;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.CredentialIssuanceMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
import io.twentysixty.sa.client.model.message.MenuItem;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.CredentialTypeResource;
import io.twentysixty.sa.res.c.MessageResource;



@ApplicationScoped
public class Service {

	private static Logger logger = Logger.getLogger(Service.class);

	@Inject EntityManager em;
	@RestClient
	@Inject
	MessageResource messageResource;
	
	@RestClient
	@Inject
	CredentialTypeResource credentialTypeResource;
	
	@ConfigProperty(name = "io.twentysixty.sa.client.debug")
	Boolean debug;
	
	@ConfigProperty(name = "io.twentysixty.emailvs.sendmail.from")
	String from;
	
	@ConfigProperty(name = "io.twentysixty.emailvs.sendmail.smtp.username")
	String username;
	
	@ConfigProperty(name = "io.twentysixty.emailvs.sendmail.smtp.password")
	String password;
	
	@ConfigProperty(name = "io.twentysixty.emailvs.sendmail.smtp.hostname")
	String hostname;
	
	@ConfigProperty(name = "io.twentysixty.emailvs.sendmail.smtp.port")
	Integer port;
	
	private static Pattern emailRegexPattern = Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
	private static HashMap<UUID, Pair<String, VerificationStep>> verifSessions = new HashMap<UUID, Pair<String, VerificationStep>>();
	private static HashMap<UUID, Pair<String, RevocationState>> revokeSessions = new HashMap<UUID, Pair<String, RevocationState> >();
	
	public static String CMD_VERIF = "/verif";
	public static String CMD_STATS = "/stats";
	public static String CMD_HELP = "/help";
	public static String CMD_CONTEXT = "/context";
	public static String CMD_VERIF_CONFIRM_YES = "/verifyes";
	public static String CMD_VERIF_CONFIRM_NO = "/verifno";
	
	private static String DATA_WELCOME =
			  "Welcome to our email verification service! Use the contextual menu to get started, or write /help for available commands";
	
	private static String DATA_ERROR =
			  "We could not understand your message. Use the contextual menu to get started, or write /help for available commands";
	
	
	
	
	private static String DATA_VERIF_INPUT_EMAIL_ERROR =
			  "This does not look like an email address. Please make sure to input a valid email address, such as user@example.com. Please input your email address";
	
	private static String DATA_VERIF_INPUT_TYPE_ERROR = "This does not look like a valid address type.";
	
	private static String DATA_VERIF_INPUT_CONFIRM_ERROR = "This does not look like a valid confirmation response.";
	
	private static String DATA_VERIF_INPUT_EMAIL_CONFIRM_TITLE =
			  "You will verify EMAIL. Confirm?";
	
	private static String DATA_VERIF_INPUT_EMAIL_CONFIRM_YES =
			  "Yes";
	
	private static String DATA_VERIF_INPUT_EMAIL_CONFIRM_NO =
			  "No";
	
	
	private static String DATA_VERIF_INPUT_CONFIRM_NO_RESTART = "You did not confirm data. Please restart the process.";
	
	private static String DATA_VERIF_INPUT_OTP =
			  "A verification code has been sent to EMAIL. Please input your verification code.";
	
	private static String DATA_VERIF_INPUT_OTP_ERROR =
			  "The verification code you input is invalid. Please input your verification code.";
	
	private static String DATA_VERIF_INPUT_OTP_OK =
			  "Congratulations, your email address EMAIL is verified! I'll send a new Verifiable Credential to you.";
	
	
	 
	private static String VERIFIED_DATA_STR_HEADER = "This is your Email Address.";
	
	
	private static String CREDENTIAL_ISSUE_NEW = "Identity complete. Issue Credential?";
	private static String CREDENTIAL_ISSUE_RE = "Identity modified. Re-Issue Credential?";
	private static String CREDENTIAL_ISSUE_CONFIRM_YES = "Yes";
	private static String CREDENTIAL_ISSUE_CONFIRM_YES_VALUE = "IC_Yes";
	private static String CREDENTIAL_ISSUE_CONFIRM_NO = "No";
	private static String CREDENTIAL_ISSUE_CONFIRM_NO_VALUE = "IC_No";
	
	
	public MenuDisplayMessage getEmailConfirmMenu(UUID connectionId, UUID threadId, String email) {


		MenuDisplayMessage confirmMenu = new MenuDisplayMessage();
		confirmMenu.setPrompt(DATA_VERIF_INPUT_EMAIL_CONFIRM_TITLE.replace("EMAIL", email));

		MenuItem yes = new MenuItem();
		yes.setId(CMD_VERIF_CONFIRM_YES);
		yes.setText(DATA_VERIF_INPUT_EMAIL_CONFIRM_YES);

		MenuItem no = new MenuItem();
		no.setId(CMD_VERIF_CONFIRM_NO);
		no.setText(DATA_VERIF_INPUT_EMAIL_CONFIRM_NO);

		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		confirmMenu.setMenuItems(menuItems);


		confirmMenu.setConnectionId(connectionId);
		confirmMenu.setThreadId(threadId);
		
		return confirmMenu;
	} 
	
	public void newConnection(UUID connectionId) {
		messageResource.sendMessage(this.getRootMenu(connectionId));
		messageResource.sendMessage(this.getWelcomeMessage(connectionId, null));
	}
	
	public TextMessage getWelcomeMessage(UUID connectionId, UUID threadId) {
		
		TextMessage message = TextMessage.build(connectionId, threadId, DATA_WELCOME);
		
		if (debug) {
			try {
				logger.info("getWelcomeMessage: " + JsonUtil.serialize(message, false));
			} catch (JsonProcessingException e) {
			}
		}
		return message;
	}

	

/*
	public BaseMessage getRootMenu(UUID connectionId) {

		ContextualMenuUpdate menu = new ContextualMenuUpdate();

		List<ContextualMenuItem> options = new ArrayList<ContextualMenuItem>();

		ContextualMenuItem verif = new ContextualMenuItem();
		verif.setId(CMD_VERIF);
		verif.setTitle("📫 Verify Email and get Credential");
		//home.setDescription("🏡 Home");
		options.add(verif);

		ContextualMenuItem revoke = new ContextualMenuItem();
		revoke.setId(CMD_REVOKE);
		revoke.setTitle("❌ Revoke Credential");
		//guess.setDescription("🔢 Guess a number 0-99");
		options.add(revoke);

		ContextualMenuItem stats = new ContextualMenuItem();
		stats.setId(CMD_STATS);
		stats.setTitle("📊 Usage Statistics");
		//fame.setDescription("🏆 Hall of Fame");
		options.add(stats);

		ContextualMenuItem help = new ContextualMenuItem();
		help.setId(CMD_HELP);
		help.setTitle("🆘 Help");
		options.add(help);

		menu.setOptions(options);
		menu.setTitle("Email Verification Service");
		menu.setDescription("Verify your email address and get a Verifiable Credential");


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
		

	}*/
	
	private String getVerifiedLabel(Verified verified) {
		StringBuffer idLabel = new StringBuffer(64);
		
		if (verified.getDeletedTs() != null) {
			idLabel.append("🧨");
		} else if (verified.getRevokedTs() != null) {
			idLabel.append("❌");
		} else if (verified.getIssuedTs() != null) {
			idLabel.append("✅");
		}else {
			idLabel.append("✏️️");
		}
		
		if (verified.getEmail() != null) {
			idLabel.append(" ").append(verified.getEmail());
		} else {
			idLabel.append(" ").append("<unset email>");
		}
		
		
		return idLabel.toString();
	}
	private static HashMap<UUID, UUID> sessions = new HashMap<UUID, UUID>();
	private static String ROOT_MENU_TITLE = "Email Verification Service";
	private static String ROOT_MENU_NO_SELECTED_ID_DESCRIPTION = "Use the contextual menu to select an Email, or verify a new one.";
	
	private static String CMD_SELECT_ID = "/select@";
	private static String CMD_ADD = "/add";
	private static String CMD_ADD_LABEL = "📫 New Email Address";
	
	private static String CMD_VIEW_ID = "/view";
	private static String CMD_VIEW_ID_LABEL = "View Email";
	
	private static String CMD_REVOKE = "/revoke";
	private static String CMD_REVOKE_LABEL = "Revoke this Email";
	
	private static String CMD_UNDELETE = "/undelete";
	private static String CMD_UNDELETE_LABEL = "Undelete this Email";
	
	private static String CMD_CONTINUE_SETUP = "/continue";
	private static String CMD_CONTINUE_SETUP_LABEL = "Continue setup";
	
	private static String CMD_UNSELECT_ID = "/unselect";
	private static String CMD_UNSELECT_ID_LABEL = "Back to main menu";
	
	
	private static String CMD_DELETE = "/delete";
	private static String CMD_DELETE_LABEL = "Delete this Email";
	
	private static String CMD_ISSUE = "/issue";
	private static String CMD_ISSUE_LABEL = "Issue Credential";
	
	
	@Transactional
	public List<Verified> getMyVerifieds(UUID connectionId) {
		Query q = em.createNamedQuery("Verified.findForConnection");
		q.setParameter("connectionId", connectionId);
		q.setParameter("deletedTs", Instant.now().minusSeconds(86400));
		return q.getResultList();
	}
	
	public BaseMessage getRootMenu(UUID connectionId) {
		
		UUID verifiedId = null;
		
		synchronized (sessions) {
			verifiedId = sessions.get(connectionId);
		}
		
		ContextualMenuUpdate menu = new ContextualMenuUpdate();
		menu.setTitle(ROOT_MENU_TITLE);
		
		List<ContextualMenuItem> options = new ArrayList<ContextualMenuItem>();

		if (verifiedId == null) {
			// build default menu
			
			menu.setDescription(ROOT_MENU_NO_SELECTED_ID_DESCRIPTION);
			List<Verified> myVerifieds = this.getMyVerifieds(connectionId);
			int i = 0;
			
			if (myVerifieds.size() != 0) {
				
				for (Verified verified: myVerifieds) {
					i++;
					String label = this.getVerifiedLabel(verified);
					String id = CMD_SELECT_ID + verified.getId();
					
					options.add(ContextualMenuItem.build(id, label, null));
				}
			}
			
			if (i<5) {
				options.add(ContextualMenuItem.build(CMD_ADD, CMD_ADD_LABEL, null));
				
			}
			
			
		} else {
			
			Verified verified = em.find(Verified.class, verifiedId);
			
			String idStr = getVerifiedLabel(verified);
			menu.setDescription(idStr.toString());
			
			if (verified.getDeletedTs() != null) {
				options.add(ContextualMenuItem.build(CMD_VIEW_ID, CMD_VIEW_ID_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNDELETE, CMD_UNDELETE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNSELECT_ID, CMD_UNSELECT_ID_LABEL, null));
			} else if (verified.getVerificationStep() != null) {
				options.add(ContextualMenuItem.build(CMD_VIEW_ID, CMD_VIEW_ID_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_CONTINUE_SETUP, CMD_CONTINUE_SETUP_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_DELETE, CMD_DELETE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNSELECT_ID, CMD_UNSELECT_ID_LABEL, null));
			} else if (verified.getRevokedTs() != null) {
				options.add(ContextualMenuItem.build(CMD_VIEW_ID, CMD_VIEW_ID_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_ISSUE, CMD_ISSUE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_DELETE, CMD_DELETE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNSELECT_ID, CMD_UNSELECT_ID_LABEL, null));
			} else if (verified.getIssuedTs() != null) {
				options.add(ContextualMenuItem.build(CMD_VIEW_ID, CMD_VIEW_ID_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_REVOKE, CMD_REVOKE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNSELECT_ID, CMD_UNSELECT_ID_LABEL, null));
			} else if (verified.getConfirmedTs() != null) {
				options.add(ContextualMenuItem.build(CMD_VIEW_ID, CMD_VIEW_ID_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_ISSUE, CMD_ISSUE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_DELETE, CMD_DELETE_LABEL, null));
				options.add(ContextualMenuItem.build(CMD_UNSELECT_ID, CMD_UNSELECT_ID_LABEL, null));
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
	
	
	public void sendmail(String to, String subject, String body) {
		
		Properties properties = System.getProperties();
		properties.put("mail.smtp.host", hostname);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        
        Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
            	return new PasswordAuthentication(username, password);
            }

        });
        
        session.setDebug(debug);

        try {
           
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            if (debug) {
            	logger.info("sendmail: email sent to " + to);
            }
        } catch (MessagingException e) {
        	logger.error("sendmail: email sent to " + to + " failed", e);
        }

    
	}
	
	
	public Pair<String, VerificationStep> getVerificationState(UUID connectionId) {
		Pair<String, VerificationStep> retval = null;
		synchronized (verifSessions) {
			retval = verifSessions.get(connectionId);
		}
		logger.info("getVerificationState: connectionId: " + connectionId + " retval " + retval);
		
		return retval;
	}
	
	public void setVerificationState(UUID connectionId, Pair<String, VerificationStep> newState) {
		
		logger.info("setVerificationState: connectionId: " + connectionId + " retval " + newState);
		
		synchronized (verifSessions) {
			verifSessions.put(connectionId, newState);
		}
	}
	
	public void removeVerificationState(UUID connectionId) {
		logger.info("removeVerificationState: removing for " + connectionId);
		synchronized (verifSessions) {
			verifSessions.remove(connectionId);
		}
	}
	public Pair<String, RevocationState>  getRevocationState(UUID connectionId) {
		Pair<String, RevocationState>  retval = null;
		synchronized (revokeSessions) {
			retval = revokeSessions.get(connectionId);
		}
		return retval;
	}
	
	public void setRevocationState(UUID connectionId, Pair<String, RevocationState> newState) {
		synchronized (revokeSessions) {
			revokeSessions.put(connectionId, newState);
		}
	}
	
	public void removeRevocationState(UUID connectionId) {
		
		synchronized (revokeSessions) {
			revokeSessions.remove(connectionId);
		}
	}
	
	
	
	private String getVerifiedDataString(Verified verified) {
		StringBuffer data = new StringBuffer(1024);
		data.append(VERIFIED_DATA_STR_HEADER).append("\n");
		
		data.append("emailAddress: ");
		
		if (verified.getEmail() != null) {
			data.append(verified.getEmail()).append("\n");
		} else {
			data.append("<unset email>").append("\n");
		}
		
		data.append("type: ");
		
		if (verified.getType() != null) {
			data.append(verified.getType()).append("\n");
		} else {
			data.append("<unset type>").append("\n");
		}
		
		
		
		
		if (verified.getVerifiedTs() != null) {
			data.append("verified: ");
			data.append(verified.getVerifiedTs()).append("\n");
		} 
		
		
		
		if (verified.getIssuedTs() != null) {
			data.append("issued: ");
			data.append(verified.getIssuedTs()).append("\n");
		} 
		
		
		return data.toString();
		
	}
	
	private String ERROR_SELECT_VERIFIED_FIRST = "⚠️ Please select an Email in the context menu in order to use this command.";
	private String VERIFIED_UNSELECTED = "Email unselected. Please check contextual menu.";
	private String VERIFIED_SELECTED = "Email selected: VERIFIED. Check the contextual menu for available actions.";
	private String ERROR_VERIFIED_UNDELETE = "⚠️ Email VERIFIED is not undeletable anymore.";
	
	private String VERIFIED_DELETED = "Email VERIFIED deleted.";
	private String VERIFIED_UNDELETED = "Email VERIFIED undeleted.";
	private String VERIFIED_REVOKED = "Email VERIFIED revoked.";
	private String ERROR_INVALID_ENTITY_STATE = "Invalid Email state. Cannot perform this action.";
	
	private static String HELP = "Use the Email Verification Service to verify your Email Addresses and get a Verifiable Credential. To get started, please check the context menu.";
	
	
	@Transactional
	public void userInput(BaseMessage message) {
		
		
		String content = null;

		
		if (message instanceof TextMessage) {
			
			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {
			
			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();
			
		} else if ((message instanceof MenuSelectMessage)) {
			
			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		}
		
		if (content == null) return;
		
		UUID verifiedId = null;
		synchronized (sessions) {
			verifiedId = sessions.get(message.getConnectionId());
		}
		Verified verified = null;
		
		if (verifiedId != null) {
			verified = em.find(Verified.class, verifiedId);
			
			if (verified != null) {
				try {
					logger.info("userInput: verified: " + JsonUtil.serialize(verified, false));
				} catch (JsonProcessingException e) {
					
				}
			}
		}
		
		
		
		
		
		if (content.equals(CMD_ADD)) {
			synchronized (sessions) {
				sessions.remove(message.getConnectionId());
			}
			
			this.entryPointCreate(message.getConnectionId(), message.getThreadId(), null);

		} else if (content.equals(CMD_HELP)) {
			messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), HELP));
		} else if (content.equals(CMD_VIEW_ID)) {
			/*
			 * ROOT MENU ACTION
			 * 
			 */
			
			if (verified != null) {
				
				String idstr = this.getVerifiedDataString(verified);
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), idstr));
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_SELECT_VERIFIED_FIRST));
			}

		} else if (content.equals(CMD_UNSELECT_ID)) {
			synchronized (sessions) {
				sessions.remove(message.getConnectionId());
				
			}
			messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), VERIFIED_UNSELECTED));
		} else if (content.equals(CMD_ISSUE)) {
			
			if (verified != null) {
				if ( (verified.getRevokedTs() != null)
				|| (
						(verified.getConfirmedTs() != null)
						
						)
				
				)
				{
					verified.setIssuedTs(Instant.now());
					verified.setRevokedTs(null);
					
					verified = em.merge(verified);
					this.sendCredential(message.getConnectionId(), verified);
					
				} else {
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_INVALID_ENTITY_STATE));
				}
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_SELECT_VERIFIED_FIRST));
			}
			
			

		} else if (content.startsWith(CMD_SELECT_ID)) {
			String[] ids = content.split("@");
			if ((ids != null) && (ids.length>1)) {
				if (ids[1] != null) {
					try {
						verifiedId = UUID.fromString(ids[1]);
					} catch (Exception e) {
					}
					if (verifiedId != null) {
						verified = em.find(Verified.class, verifiedId);
						if (verified != null) {
							synchronized (sessions) {
								sessions.put(message.getConnectionId(), verifiedId);
							}
							messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), VERIFIED_SELECTED.replace("VERIFIED", this.getVerifiedLabel(verified))));
						}
					}
				}
			}
			
		} else if (content.equals(CMD_CONTINUE_SETUP)) {
			
			this.entryPointCreate(message.getConnectionId(), message.getThreadId(), null);

		} else if (content.equals(CMD_UNDELETE)) {
			if (verified != null) {
				if (verified.getDeletedTs().plusSeconds(86400).compareTo(Instant.now())>0) {
					verified.setDeletedTs(null);
					verified = em.merge(verified);
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), VERIFIED_UNDELETED.replace("VERIFIED", this.getVerifiedLabel(verified))));

				} else {
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_VERIFIED_UNDELETE.replace("IDENTITY", this.getVerifiedLabel(verified))));
				}
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_SELECT_VERIFIED_FIRST));
			}

		} else if (content.equals(CMD_DELETE)) {
			if (verified != null) {
				if ( (verified.getIssuedTs() == null)
				|| (verified.getRevokedTs() != null)
						)
				{
					verified.setDeletedTs(Instant.now());
					verified = em.merge(verified);
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), VERIFIED_DELETED.replace("VERIFIED", this.getVerifiedLabel(verified))));
				}
				synchronized (sessions) {
					sessions.remove(message.getConnectionId());
				}
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_SELECT_VERIFIED_FIRST));
			}

		} else if (content.equals(CMD_REVOKE)) {
			if (verified != null) {
				if (verified.getIssuedTs() != null) {
					verified.setRevokedTs(Instant.now());
					verified = em.merge(verified);
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), VERIFIED_REVOKED.replace("VERIFIED", this.getVerifiedLabel(verified))));
				}
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), ERROR_SELECT_VERIFIED_FIRST));
			}

		} else {
			if (verified != null) {
				if (!verified.getVerificationStep().equals(VerificationStep.FINISHED)) {
					this.entryPointCreate(message.getConnectionId(), message.getThreadId(), content);
				} else {
					messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_ERROR));
				}
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_ERROR));
			}
			
		}
		
		messageResource.sendMessage(this.getRootMenu(message.getConnectionId()));
	}
	
	// STARTED, EMAIL_OK, OTP, FINISHED
	
	
	private static String WELCOME = "Welcome to our Email Verification Service.";
	
	private static String EMAIL_REQUEST = "Let's verify your mailbox. First, please input your Email Address.";
	private static String TYPE_REQUEST = "Is this email a Private Email Address (controlled only by you) or a Group Email Address (someone else can read emails sent to this address)?";
	private static String TYPE_REQUEST_HEADER = "Select if Private or Group Email Address";
	private static String TYPE_REQUEST_PRIVATE = "Private Email Address";
	private static String TYPE_REQUEST_PRIVATE_VALUE = "/privateType";
	private static String TYPE_REQUEST_GROUP = "Group Email Address";
	private static String TYPE_REQUEST_GROUP_VALUE = "/groupType";
	private static String NOT_ISSUED_INSTRUCTIONS = "You can request issue of a credential at any time my using the context menu or by writing the command /issue";

	private BaseMessage getIssueConfirmMenu(UUID connectionId, UUID threadId, boolean newCredential) {

		MenuDisplayMessage confirm = new MenuDisplayMessage();
		if (newCredential) {
			confirm.setPrompt(CREDENTIAL_ISSUE_NEW);
		} else {
			confirm.setPrompt(CREDENTIAL_ISSUE_RE);
		}
		

		MenuItem yes = new MenuItem();
		yes.setId(CREDENTIAL_ISSUE_CONFIRM_YES_VALUE);
		yes.setText(CREDENTIAL_ISSUE_CONFIRM_YES);
		
		MenuItem no = new MenuItem();
		no.setId(CREDENTIAL_ISSUE_CONFIRM_NO_VALUE);
		no.setText(CREDENTIAL_ISSUE_CONFIRM_NO);
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		
		confirm.setMenuItems(menuItems);


		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		
		return confirm;
	} 




	
	
	private BaseMessage getConfirmPrivateGroup(UUID connectionId, UUID threadId) {

		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(TYPE_REQUEST_HEADER);

		MenuItem yes = new MenuItem();
		yes.setId(TYPE_REQUEST_PRIVATE_VALUE);
		yes.setText(TYPE_REQUEST_PRIVATE);
		
		MenuItem no = new MenuItem();
		no.setId(TYPE_REQUEST_GROUP_VALUE);
		no.setText(TYPE_REQUEST_GROUP);
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		
		confirm.setMenuItems(menuItems);


		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		
		return confirm;
	} 
	
	private static String CONFIRM_REQUEST_HEADER = "Confirm data?";
	private static String CONFIRM_REQUEST_YES = "Yes";
	private static String CONFIRM_REQUEST_YES_VALUE = "/confirmYes";
	private static String CONFIRM_REQUEST_NO = "No";
	private static String CONFIRM_REQUEST_NO_VALUE = "/confirmNo";
	
	
	
	private BaseMessage getConfirmAll(UUID connectionId, UUID threadId) {

		MenuDisplayMessage confirm = new MenuDisplayMessage();
		confirm.setPrompt(CONFIRM_REQUEST_HEADER);

		MenuItem yes = new MenuItem();
		yes.setId(CONFIRM_REQUEST_YES_VALUE);
		yes.setText(CONFIRM_REQUEST_YES);
		
		MenuItem no = new MenuItem();
		no.setId(CONFIRM_REQUEST_NO_VALUE);
		no.setText(CONFIRM_REQUEST_NO);
		
		List<MenuItem> menuItems = new ArrayList<MenuItem>();
		menuItems.add(yes);
		menuItems.add(no);
		
		
		confirm.setMenuItems(menuItems);


		confirm.setConnectionId(connectionId);
		confirm.setThreadId(threadId);
		
		return confirm;
	} 
	
	private void sendMessage(UUID connectionId, UUID threadId, Verified verified) {
		if (verified.getVerificationStep() == null) {
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, WELCOME));
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, EMAIL_REQUEST));
		} else switch (verified.getVerificationStep()) {
		case EMAIL: {
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, EMAIL_REQUEST));
			break;
		} 
		case TYPE: {
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, TYPE_REQUEST));
			messageResource.sendMessage(this.getConfirmPrivateGroup(connectionId, threadId));
			break;
		}
		case CONFIRM: {
			String dataStr = this.getVerifiedDataString(verified);
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, dataStr));
			messageResource.sendMessage(this.getConfirmAll(connectionId, threadId));
			break;
		}
		case OTP: {
			messageResource.sendMessage(TextMessage.build(connectionId, threadId, DATA_VERIF_INPUT_OTP.replaceFirst("EMAIL", verified.getEmail())));
			break;
		}
		
		case ISSUE: {
			messageResource.sendMessage(this.getIssueConfirmMenu(connectionId, threadId, true));
			break;
		}
		
		}
	}

	
	
	private String genOtp() {
		int leftLimit = 48; // numeral '0'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;
	    Random random = new Random();

	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();
	    return generatedString;
	}
	
	private void entryPointCreate(UUID connectionId, UUID threadId, String content) {
		
		UUID verifiedId = null;
		Verified verified = null;
		String normalizedContent = null;
		if (content != null) {
			normalizedContent = content.strip();
			if (normalizedContent.length()==0) normalizedContent = null;
		}
		synchronized (sessions) {
			verifiedId = sessions.get(connectionId);
		}
		
		if (verifiedId == null) {
			// new flow
			verified = new Verified();
			verified.setConnectionId(connectionId);
			verified.setStartedTs(Instant.now());
			verified.setId(UUID.randomUUID());
			
			em.persist(verified);
			verifiedId = verified.getId();
			synchronized (sessions) {
				sessions.put(connectionId, verifiedId);
			}
			
		} else {
			verified = em.find(Verified.class, verifiedId);
		}
		
			if (verified.getVerificationStep() == null) {
				messageResource.sendMessage(TextMessage.build(connectionId, threadId, WELCOME));
				verified.setVerificationStep(VerificationStep.EMAIL);
				verified = em.merge(verified);
				this.sendMessage(connectionId, threadId, verified);
			} else switch (verified.getVerificationStep()) {
				
				case EMAIL: {
					if (normalizedContent != null) {
						String possibleEmail = normalizedContent.toLowerCase(Locale.ROOT);
						if (this.isValidEmail(possibleEmail)) {
							verified.setVerificationStep(VerificationStep.TYPE);
							verified.setEmail(possibleEmail);
							verified = em.merge(verified);
						} else { 
							messageResource.sendMessage(TextMessage.build(connectionId, threadId, DATA_VERIF_INPUT_EMAIL_ERROR));
						}
						
					}
					this.sendMessage(connectionId, threadId, verified);
					break;
				} 
				case TYPE: {
					if (normalizedContent != null) {
						if (normalizedContent.equals(TYPE_REQUEST_PRIVATE_VALUE)) {
							verified.setType(EmailType.PRIVATE);
							verified.setVerificationStep(VerificationStep.CONFIRM);
							verified = em.merge(verified);
						} else if (normalizedContent.equals(TYPE_REQUEST_GROUP_VALUE)) {
							verified.setType(EmailType.GROUP);
							verified.setVerificationStep(VerificationStep.CONFIRM);
							verified = em.merge(verified);
						} else {
							messageResource.sendMessage(TextMessage.build(connectionId, threadId, DATA_VERIF_INPUT_TYPE_ERROR));

						}
						
					}
					this.sendMessage(connectionId, threadId, verified);
					break;
				}
				case CONFIRM: {
					if (normalizedContent != null) {
						
						if (normalizedContent.equals(CONFIRM_REQUEST_YES_VALUE)) {
							verified.setConfirmedTs(Instant.now());
							verified.setVerificationStep(VerificationStep.OTP);
							verified.setOtp(this.genOtp());
							verified = em.merge(verified);
							
							this.sendmail(verified.getEmail(), "OTP code for Email verification", "Your OTP is: " + verified.getOtp());

						} else if (normalizedContent.equals(CONFIRM_REQUEST_NO_VALUE)) {
							verified.setConfirmedTs(null);
							verified.setVerifiedTs(null);
							verified.setVerificationStep(VerificationStep.EMAIL);
							verified.setOtp(null);
							verified.setEmail(null);
							verified.setType(null);
							verified = em.merge(verified);
							
							messageResource.sendMessage(TextMessage.build(connectionId, threadId, DATA_VERIF_INPUT_CONFIRM_NO_RESTART));
							
						} else {
							messageResource.sendMessage(TextMessage.build(connectionId, threadId, DATA_VERIF_INPUT_CONFIRM_ERROR));

						}
						
						
						
					}
					this.sendMessage(connectionId, threadId, verified);
					break;
				}
				case OTP: {
					if (normalizedContent != null) {
						
						if ((verified.getOtp() != null) && normalizedContent.equals(verified.getOtp())) {
								verified.setVerifiedTs(Instant.now());
								verified.setVerificationStep(VerificationStep.ISSUE);
								verified = em.merge(verified);
								// 	@NamedQuery(name="Verified.purgeOld", query="UPDATE Verified i set i.revokedTs=:revokedTs, i.verifiedTs=:verifiedTs, i.deletedTs=:deletedTs where i.connectionId<>:connectionId and i.email=:email"),

								if (verified.getType().equals(EmailType.PRIVATE)) {
									// revoke old, as it is private
									Query q = em.createNamedQuery("Verified.purgeOld");
									q.setParameter("email", verified.getEmail());
									q.setParameter("connectionId", verified.getConnectionId());
									q.setParameter("revokedTs", Instant.now());
									q.setParameter("verifiedTs", null);
									q.setParameter("deletedTs", Instant.now());
									q.executeUpdate();
									
								}
								
								messageResource.sendMessage(
										TextMessage.build(connectionId, 
												threadId, 
												DATA_VERIF_INPUT_OTP_OK.replace("EMAIL", verified.getEmail()))
												);
								
								
							
						} else {
							messageResource.sendMessage(
									TextMessage.build(connectionId, 
											threadId, 
											DATA_VERIF_INPUT_OTP_ERROR
											)
											);
							
						}
						
						
					}
					
					this.sendMessage(connectionId, threadId, verified);
					
					
					break;
				}
				case ISSUE: {
					if (normalizedContent != null) {
						
						if (normalizedContent.equals(CREDENTIAL_ISSUE_CONFIRM_YES_VALUE)) {
							
							
							
							verified.setVerificationStep(VerificationStep.FINISHED);
							verified.setIssuedTs(Instant.now());
							verified = em.merge(verified);
							
							this.sendCredential(connectionId, verified);
						} else if (normalizedContent.equals(CREDENTIAL_ISSUE_CONFIRM_NO_VALUE)) {
							verified.setVerificationStep(VerificationStep.FINISHED);
							verified.setIssuedTs(null);
							verified = em.merge(verified);
							
							
							messageResource.sendMessage(
									TextMessage.build(connectionId, 
											threadId, 
											NOT_ISSUED_INSTRUCTIONS
											)
											);
						}
						
						
						
						
					} else {
						this.sendMessage(connectionId, threadId, verified);
					}
					break;
				}
				
			}
			
		
		
	}

	/*
	public void userInput(BaseMessage message) {
		
		if (message == null) return;

		String content = null;

		
		if (message instanceof TextMessage) {
			
			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();

		} else if ((message instanceof ContextualMenuSelect) ) {
			
			ContextualMenuSelect menuSelect = (ContextualMenuSelect) message;
			content = menuSelect.getSelectionId();
			messageResource.sendMessage(this.getRootMenu(message.getConnectionId()));

		} else if ((message instanceof MenuSelectMessage)) {
			
			MenuSelectMessage menuSelect = (MenuSelectMessage) message;
			content = menuSelect.getMenuItems().iterator().next().getId();
		}
		
		if (content != null) {
			if (content.strip().startsWith(CMD_VERIF)) {
				this.setVerificationState(message.getConnectionId(), Pair.create(null, VerificationStep.STARTED));
				this.removeRevocationState(message.getConnectionId());
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_VERIF_INPUT_EMAIL));
			} else if (content.equals(CMD_REVOKE)) {
				this.removeVerificationState(message.getConnectionId());
				this.setRevocationState(message.getConnectionId(), Pair.create(null, RevocationState.STARTED));
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_REVOKE_INPUT_CREDENTIAL));
			} else if (content.equals(CMD_STATS)) {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), getStatsMessage()));
			} else if (content.equals(CMD_HELP)) {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_HELP));
			} else if (content.equals(CMD_CONTEXT)) {
				messageResource.sendMessage(this.getRootMenu(message.getConnectionId()));
			} else if (this.getVerificationState(message.getConnectionId()) != null) {
				Pair<String, VerificationStep> state = getVerificationState(message.getConnectionId());

				switch (state.getRight()) {
				case STARTED: {
					// expect email
					String data = content.strip();
					if (this.isValidEmail(data)) {
						this.setVerificationState(message.getConnectionId(), Pair.create(data, VerificationStep.EMAIL_OK));
						messageResource.sendMessage(getEmailConfirmMenu(message.getConnectionId(), message.getThreadId(), data));
					} else {
						messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_VERIF_INPUT_EMAIL_ERROR));
					}
					break;
				}
				case EMAIL_OK: {

					if (content.strip().startsWith(CMD_VERIF_CONFIRM_YES)) {
						this.setVerificationState(message.getConnectionId(), Pair.create(state.getLeft(), VerificationStep.OTP));
						Verified v = verifiedDao.startVerification(state.getLeft(), message.getConnectionId());
						this.sendmail(v.getEmail(), "OTP code for email verification", "Your otp is: " + v.getOtp());
						messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_VERIF_INPUT_OTP.replace("EMAIL", v.getEmail())));
					} else if (content.strip().startsWith(CMD_VERIF_CONFIRM_NO)) {
						this.setVerificationState(message.getConnectionId(), Pair.create(null, VerificationStep.STARTED));
						messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_VERIF_INPUT_EMAIL));
					} else {
						messageResource.sendMessage(getEmailConfirmMenu(message.getConnectionId(), message.getThreadId(), state.getLeft()));
					}
					break;
				}
				case OTP: {
					boolean verified = verifiedDao.validateOtp(state.getLeft(), message.getConnectionId(), content);

					if (verified) {
						this.removeVerificationState(message.getConnectionId());
						
						messageResource.sendMessage(
								TextMessage.build(message.getConnectionId(), 
										message.getThreadId(), 
										DATA_VERIF_INPUT_OTP_OK.replace("EMAIL", state.getLeft()))
										);

						this.sendCredential(message.getConnectionId(), state.getLeft());

					} else {
						messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_VERIF_INPUT_OTP_ERROR));
					}

				}
				}

			} else if (this.getRevocationState(message.getConnectionId()) != null) {
				Pair<String, RevocationState> state = getRevocationState(message.getConnectionId());
				
			} else {
				messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_ERROR));

			}
		}

	}*/
	
	private void sendCredential(UUID connectionId, Verified verified) {
		
		if (verified == null) return;
		
		CredentialIssuanceMessage cred = new CredentialIssuanceMessage();
		cred.setConnectionId(connectionId);
		cred.setCredentialDefinitionId(getCredentialType().getId());
		
		Claim id = new Claim();
		id.setName("id");
		id.setValue(verified.getId().toString());
		
		
		Claim emailClaim = new Claim();
		emailClaim.setName("emailAddress");
		emailClaim.setValue(verified.getEmail());
		
		Claim confirmed = new Claim();
		confirmed.setName("verifiedTs");
		confirmed.setValue(verified.getVerifiedTs().toString());
		
		Claim issued = new Claim();
		issued.setName("issuedTs");
		issued.setValue(verified.getIssuedTs().toString());
		
		Claim type = new Claim();
		type.setName("type");
		type.setValue(verified.getType().toString());
		
		
		
		List<Claim> claims = new ArrayList<Claim>();
		claims.add(id);
		claims.add(emailClaim);
		claims.add(type);
		claims.add(confirmed);
		claims.add(issued);
		
		
		
		cred.setClaims(claims);
		try {
			logger.info("sendCredential: " + JsonUtil.serialize(cred, false));
		} catch (JsonProcessingException e) {
		}
		messageResource.sendMessage(cred);
	}

	private static CredentialType type = null;
	private Object lockObj = new Object();
	
	private CredentialType getCredentialType() {
		synchronized (lockObj) {
			if (type == null) {
				List<CredentialType> types = credentialTypeResource.getAllCredentialTypes();
				
				if ( (types == null) || (types.size()==0) ) {
					
					CredentialType newType = new CredentialType();
					newType.setName("emailAddress");
					newType.setVersion("1.0");
					List<String> attributes = new ArrayList<String>();
					attributes.add("id");
					attributes.add("emailAddress");
					attributes.add("verifiedTs");
					attributes.add("type");
					attributes.add("issuedTs");
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
	

	private boolean isValidEmail(String to) {
		return emailRegexPattern.matcher(to)
			      .matches();
	}

	private String getStatsMessage() {
		
		return "Stats: Not implemented";
	}
}
