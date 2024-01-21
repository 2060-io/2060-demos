package io.twentysixty.demos.helloworld.res.s;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.demos.helloworld.svc.Service;
import io.twentysixty.sa.client.model.event.ConnectionStateUpdated;
import io.twentysixty.sa.client.model.event.DidExchangeState;
import io.twentysixty.sa.client.res.s.ConnectionEventInterface;
import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("")
public class ConnectionEventResource implements ConnectionEventInterface {

	
	private static Logger logger = Logger.getLogger(ConnectionEventResource.class);

	@Inject Service service;
	@ConfigProperty(name = "io.twentysixty.sa.client.debug")
	Boolean debug;

	@Override
	@POST
	@Path("/connection-state-updated")
	@Produces("application/json")
	public Response connectionStateUpdated(ConnectionStateUpdated event) {
		if (debug) {
			try {
				logger.info("connectionStateUpdated: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		if (event.getState().equals(DidExchangeState.COMPLETED)) {
			try {
				service.newConnection(event.getConnectionId());
			} catch (Exception e) {
				
				logger.error("", e);
				return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
		return  Response.status(Status.OK).build();
		
		
	}

}
