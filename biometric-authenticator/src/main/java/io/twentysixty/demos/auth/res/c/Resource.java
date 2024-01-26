package io.twentysixty.demos.auth.res.c;

import java.io.InputStream;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class Resource {

	@FormParam("chunk")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream chunk;
}