package main.back.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Response;
import main.back.location.model.Location;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
@ServerEndpoint("/location")
public class LocationWebSocketEndpoint {
    private static final Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LocationController controller = new LocationController();
        Response response = controller.getLocations();
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(response.getEntity());
            session.getBasicRemote().sendText(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("Отключился пользователь: " + session.toString());
    }

    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message) {
        sendMessageToAll();
    }

    private void sendMessageToAll() {
        LocationController controller = new LocationController();
        Response response = controller.getLocations();
        ObjectMapper mapper = new ObjectMapper();

        sessions.forEach(s -> {
            try {
                String jsonString = mapper.writeValueAsString(response.getEntity());
                s.getBasicRemote().sendText(jsonString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}