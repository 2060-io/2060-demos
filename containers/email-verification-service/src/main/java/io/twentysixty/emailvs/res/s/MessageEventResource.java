package io.twentysixty.emailvs.res.s;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.emailvs.svc.Service;
import io.twentysixty.sa.client.model.event.MessageReceived;
import io.twentysixty.sa.client.model.event.MessageState;
import io.twentysixty.sa.client.model.event.MessageStateUpdated;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.res.s.MessageEventInterface;
import io.twentysixty.sa.client.util.JsonUtil;
import io.twentysixty.sa.res.c.MessageResource;




@Path("")
public class MessageEventResource implements MessageEventInterface {

	private static Logger logger = Logger.getLogger(MessageEventResource.class);

	@RestClient
	@Inject
	MessageResource messageResource;


	@Inject Service service;

	@ConfigProperty(name = "io.twentysixty.sa.client.debug")
	Boolean debug;

	@Override
	@POST
	@Path("/message-received")
	@Produces("application/json")
	public Response messageReceived(MessageReceived event) {
		

		
		if (debug) {
			try {
				logger.info("messageReceived: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		
		List<MessageReceiptOptions> receipts = new ArrayList<MessageReceiptOptions>();
		
		/*MessageReceiptOptions received = new MessageReceiptOptions();
		received.setMessageId(event.getMessage().getId());
		received.setTimestamp(Instant.now());
		received.setState(MessageState.RECEIVED);
		receipts.add(received);
		*/
		
		MessageReceiptOptions viewed = new MessageReceiptOptions();
		viewed.setMessageId(event.getMessage().getId());
		viewed.setTimestamp(Instant.now());
		viewed.setState(MessageState.VIEWED);
		receipts.add(viewed);
		
		ReceiptsMessage r = new ReceiptsMessage();
		r.setConnectionId(event.getMessage().getConnectionId());
		r.setReceipts(receipts);
		
		try {
			messageResource.sendMessage(r);
			
			
			//messageResource.sendMessage(r);
		} catch (Exception e) {
			logger.error("", e);
		}
		
		
		if (debug) {
			try {
				logger.info("messageReceived: sent receipts:" + JsonUtil.serialize(r, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		try {
			service.userInput(event.getMessage());
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return  Response.status(Status.OK).build();
		
		
	}

	@Override
	@POST
	@Path("/message-state-updated")
	@Produces("application/json")
	public Response messageStateUpdated(MessageStateUpdated event) {
		
		if (debug) {
			try {
				logger.info("messageStateUpdated: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		return  Response.status(Status.OK).build();
		
	}

	

}
