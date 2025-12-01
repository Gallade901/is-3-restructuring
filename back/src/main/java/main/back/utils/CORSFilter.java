package main.back.utils;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");

        if (isAllowedOrigin(origin)) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
        }

        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS, PATCH");
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, Authorization, Content-Length, X-Requested-With, Origin, Accept, " +
                        "Content-Disposition, Cache-Control");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
        responseContext.getHeaders().add("Access-Control-Expose-Headers",
                "Content-Disposition, Content-Type, Content-Length");

        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            responseContext.setStatus(Response.Status.OK.getStatusCode());
        }
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return origin.equals("https://se.ifmo.ru") ||
                origin.startsWith("http://localhost") ||
                origin.startsWith("http://127.0.0.1");
    }
}