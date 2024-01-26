package io.gaiaid.registry.res.s;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

import io.gaiaid.registry.ex.MediaAlreadyLinkedException;
import io.gaiaid.registry.ex.TokenException;
import io.gaiaid.registry.svc.VisionService;

@Path("")
public class VisionResource {

	
	private static Logger logger = Logger.getLogger(VisionResource.class);

	@Inject VisionService service;
	@ConfigProperty(name = "io.gaiaid.debug")
	Boolean debug;

	
	
	
	@PUT
	@Path("/link/{token}/{mediaId}")
	@Operation(summary = "Link a media",
	description = "Link a media of type {type} with UUID {mediaId} to identity represented by token {token}")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments or expired token"),
		@APIResponse(responseCode = "403", description = "Permission Denied."),
		@APIResponse(responseCode = "500", description = "Server error, please retry."),

		@APIResponse(responseCode = "201", description = "Media successfully linked.") }
	)
	public Response linkMedia(@PathParam(value = "token") UUID token, 
			 @PathParam(value = "mediaId") UUID mediaId) {
		
		if (token == null || mediaId == null) {
			return  Response.status(Status.BAD_REQUEST).build();
		}
		
		try {
			service.linkMedia(token, mediaId);
			return Response.status(Status.CREATED).build();
		} catch (TokenException e) {
			logger.error("", e);
			return  Response.status(Status.BAD_REQUEST).build();
		} catch (MediaAlreadyLinkedException e) {
			logger.error("", e);
			return  Response.status(Status.FORBIDDEN).build();
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		
	}
	
	@GET
	@Path("/list/{token}")
	@Produces("application/json")
	@Operation(summary = "List ids (UUID) of medias",
	description = "List of mediaIds (UUID) of type {type} linked to identity represented by token {token}")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments or expired token"),
		@APIResponse(responseCode = "403", description = "Permission Denied."),
		@APIResponse(responseCode = "500", description = "Server error, please retry."),

		@APIResponse(responseCode = "200", description = "OK") }
	)
	public Response listMedias(@PathParam(value = "token") UUID token) {
		
		if (token == null) {
			return  Response.status(Status.BAD_REQUEST).build();
		}
		
		try {
			List<UUID> medias = service.listMedias(token);
			return Response.status(Status.OK).entity(medias).build();
		} catch (TokenException e) {
			logger.error("", e);
			return  Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		
		
	}
	
	
	@PUT
	@Path("/success/{token}")
	@Operation(summary = "Notify a successful verification or capture",
	description = "Notify a successful verification or capture for identity represented by token {token}.")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments or expired token."),
		@APIResponse(responseCode = "403", description = "Permission Denied."),
		@APIResponse(responseCode = "500", description = "Server error, please retry."),

		@APIResponse(responseCode = "200", description = "OK") }
	)
	public Response success(@PathParam(value = "token") UUID token) {
		
		if (token == null) {
			return  Response.status(Status.BAD_REQUEST).build();
		}
		
		try {
			service.success(token);
			
			return Response.status(Status.OK).build();
		} catch (TokenException e) {
			logger.error("", e);
			return  Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		
	}
	
	@PUT
	@Path("/failure/{token}")
	@Operation(summary = "Notify a failed verification or capture",
	description = "Notify a failed verification or capture for Identity represented by token {token}.")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments or expired token."),
		@APIResponse(responseCode = "403", description = "Permission Denied."),
		@APIResponse(responseCode = "500", description = "Server error, please retry."),

		@APIResponse(responseCode = "200", description = "OK") }
	)
	public Response failure(@PathParam(value = "token") UUID token) {
		
		if (token == null ) {
			return  Response.status(Status.BAD_REQUEST).build();
		}
		
		try {
			service.failure(token);
			
			return Response.status(Status.OK).build();
		} catch (TokenException e) {
			logger.error("", e);
			return  Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		
	}
	
	
}


