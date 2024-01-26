package io.gaiaid.registry.res.c;

import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;


@RegisterRestClient
@Path("")
public interface MediaResource {

	
	@GET
	@Path("/r/{uuid}")
	public byte[] render(@PathParam(value = "uuid") UUID uuid);
	
	
	@PUT
	@Path("/c/{uuid}/{noc}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void createOrUpdate(@PathParam(value = "uuid") UUID uuid, 
			@PathParam(value = "noc") Integer numberOfChunks,
			@QueryParam(value = "token") String token);
		
	
	@PUT
	@Path("/u/{uuid}/{c}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void uploadChunk( @PathParam(value = "uuid") UUID uuid, 
    		@PathParam(value = "c") Integer partNumber,
    		@QueryParam(value = "token") String token,
    		@MultipartForm Resource data);
	
	@DELETE
	@Path("/d/{uuid}")
	public void delete( @PathParam(value = "uuid") UUID uuid,
			@QueryParam(value = "token") String token);
	
}
