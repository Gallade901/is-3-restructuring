package main.back.utils;


import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import main.back.user.controller.UserController;


import java.io.IOException;
import java.util.Map;

@Provider
@PreMatching
public class AuthorizationFilter implements ContainerRequestFilter {

    @Inject
    private UserController userController;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String currentPath = requestContext.getUriInfo().getPath();

        // Пропускаем публичные эндпоинты
        if (isPublicEndpoint(currentPath) || isJmeterRequest(requestContext)) {
            return;
        }

        Map<String, Cookie> cookies = requestContext.getCookies();
        Cookie sessionIdCookie = cookies.get("sessionId");
        String sessionId = sessionIdCookie != null ? sessionIdCookie.getValue() : null;

        if (sessionId == null || sessionId.trim().isEmpty()) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("No session")
                            .build()
            );
            return;
        }

        Response response = userController.checkAuthorization(sessionId);
        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            requestContext.abortWith(response);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return "/user/registration".equals(path) ||
                "/user/authorization".equals(path) ||
                "/user/checkAuthorization".equals(path);
    }
    private boolean isJmeterRequest(ContainerRequestContext requestContext) {
        String userAgent = requestContext.getHeaderString("User-Agent");
        return userAgent != null && userAgent.contains("Apache-HttpClient");
    }
}