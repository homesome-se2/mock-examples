package com.homesome.web_resource;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import com.homesome.service.ClientHandler;

/**
 * https://www.youtube.com/watch?v=wpzQa2J0YM8
 * https://sparktutorials.github.io/2015/11/08/spark-websocket-chat.html
 * https://sparktutorials.github.io/2015/04/02/setting-up-a-spark-project-with-maven.html
 */

@WebSocket // Annotate that this is a WebSocket class
public class WebSocketServer {

    // New client connected to WebSocket server
    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        session.setIdleTimeout(60*1000); // Server closes session (connection) if idle.
        ClientHandler.getInstance().addClient(session);
    }

    // Client disconnected from WebSocket server
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) throws Exception{
        ClientHandler.getInstance().removeClient(session);
    }

    // Message from client
    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        ClientHandler.getInstance().addClientRequest(session, message);
    }
}
