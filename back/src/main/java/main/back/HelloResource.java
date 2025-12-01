package main.back;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.logging.Logger;

@Path("/hello-world")
public class HelloResource {
    private static final Logger logger = Logger.getLogger(HelloResource.class.getName());

    @GET
    @Produces("text/plain")
    public String hello() {
        return "Hello, World!";
    }
}