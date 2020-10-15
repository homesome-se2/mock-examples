package com.homesome.web_resources;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerConnection {

    // Queue holding incoming commands from server. Processed in class ClientApp
    public BlockingQueue<String> serverCommands;
    // Instance representing the session being established between client and server.
    private Session session;
    private volatile boolean connectedToServer;

    // Temp holder. Send and erase when connection is established.
    private String loginRequest;

    private final Object lockObject_output;
    private final Object lockObject_close;

    // Make Singleton
    private static ServerConnection instance = null;

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private ServerConnection(){
        serverCommands = new ArrayBlockingQueue<>(10);
        session = null;
        connectedToServer = false;
        lockObject_output = new Object();
        lockObject_close = new Object();
    }

    // Called from class ClientApp
    public void connectToServer(String loginRequest) {
        this.loginRequest = loginRequest;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            String uri = "ws://134.209.198.123:8084/homesome";
            // String uri = "ws://localhost:8084/homesome";
            container.connectToServer(WebSocketClient.class, URI.create(uri)); // returns a WebSocket session object
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Called from class ClientApp
    public void closeConnection() {
        synchronized (lockObject_close) {
            if (connectedToServer) {
                connectedToServer = false;
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                    System.out.println("Server session closed");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Called from class WebSocketClient
    public void onServerConnect(Session session) {
        System.out.println("Connected to server");
        this.session = session;
        connectedToServer = true;
        // Send login request
        writeToServer(loginRequest);
    }

    // Called from class WebSocketClient
    public void onServerClose() {
        connectedToServer = false;
        System.out.println("Server closed the session");
    }

    // Called from class ClientApp
    public void writeToServer(String msg) {
        synchronized (lockObject_output) {
            System.out.println("Msg to server: " + msg);
            try {
                if (connectedToServer && session.isOpen()) {
                    session.getBasicRemote().sendText(msg);
                } else {
                    System.out.println("Not connected to the server");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Called from class WebSocketClient
    public void newCommandFromServer(String msg) {
        System.out.println("Msg from server: " + msg);
        try {
            serverCommands.put(msg);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    //TODO: Here or in WebSocket implementation class: Scheduler/timer to send a ping msg to server at intervals.
    // Server closes client connections after 1 min idle = Clients must break the idle state periodically.
}
