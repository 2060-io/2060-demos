package io.twentysixty.demos.helloworld.svc;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.sa.client.model.credential.CredentialType;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.Claim;
import io.twentysixty.sa.client.model.message.CredentialIssuanceMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.CredentialTypeResource;
import io.twentysixty.sa.res.c.MessageResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;



@ApplicationScoped
public class Service {

	private static Logger logger = Logger.getLogger(Service.class);

	@RestClient
	@Inject
	MessageResource messageResource;
	
	@RestClient
	@Inject
	CredentialTypeResource credentialTypeResource;
	
	@ConfigProperty(name = "io.twentysixty.sa.client.debug")
	Boolean debug;
	
	
	private static String DATA_WELCOME =
			  "Welcome to hello world service! Use the contextual menu to get started, or write /help for available commands";
	
	private static String DATA_RECEIVED =
			  "I received your message";
	
	
	private static CredentialType type = null;
	private static Object lockObj = new Object();
	private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	
	
	public void userInput(BaseMessage message) {
		
		
		String content = null;

		
		if (message instanceof TextMessage) {
			
			TextMessage textMessage = (TextMessage) message;
			content = textMessage.getContent();
			
			messageResource.sendMessage(TextMessage.build(message.getConnectionId(), message.getThreadId(), DATA_RECEIVED));

		}
	}





	public void newConnection(UUID connectionId) {
		messageResource.sendMessage(TextMessage.build(connectionId, null, DATA_WELCOME));

		
	}
	
	private void sendCredential(UUID connectionId) throws Exception {
		
		CredentialIssuanceMessage cred = new CredentialIssuanceMessage();
		cred.setConnectionId(connectionId);
		cred.setCredentialDefinitionId(getCredentialType().getId());
		
		
		List<Claim> claims = new ArrayList<Claim>();
		
		Claim idId = new Claim();
		idId.setName("id");
		idId.setValue(UUID.randomUUID().toString());
		claims.add(idId);
		
		Claim firstname = new Claim();
		firstname.setName("name");
		firstname.setValue("Alice");
		claims.add(firstname);
		
		Claim issued = new Claim();
		issued.setName("issued");
		issued.setValue(Instant.now().toString());
		
		
		claims.add(issued);
		cred.setClaims(claims);
		try {
			logger.info("sendCredential: " + JsonUtil.serialize(cred, false));
		} catch (JsonProcessingException e) {
		}
		messageResource.sendMessage(cred);
	}
	
	
	private CredentialType getCredentialType() {
	synchronized (lockObj) {
		if (type == null) {
			List<CredentialType> types = credentialTypeResource.getAllCredentialTypes();
			
			if ( (types == null) || (types.size()==0) ) {
				
				CredentialType newType = new CredentialType();
				newType.setName("Hello World!");
				newType.setVersion("1.0");
				
				List<String> attributes = new ArrayList<String>();
				attributes.add("id");
				attributes.add("name");
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


}
