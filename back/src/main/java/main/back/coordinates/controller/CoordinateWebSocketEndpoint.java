package main.back.coordinates.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.*;

@ApplicationScoped
@ServerEndpoint("/coordinate")
public class CoordinateWebSocketEndpoint {
    private static final Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        CoordinatesController controller = new CoordinatesController();
        Response response = controller.getCoordinates();
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
        CoordinatesController controller = new CoordinatesController();
        Response response = controller.getCoordinates();
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