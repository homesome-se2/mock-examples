package com.homesome.web_resources;

import com.homesome.service.Hub;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;

public class ServerConnection {

    // Instance representing the session being established between client and server.
    private Session session;
    private volatile boolean connectedToServer;
    public volatile boolean loggedInToServer;

    // Session management: Ping and reconnect
    private Thread manageConnThread;

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
        session = null;
        connectedToServer = false;
        loggedInToServer = false;
        lockObject_output = new Object();
        lockObject_close = new Object();
        manageConnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    manageConnection();
                } catch (Exception e) {
                    System.out.println("Server connection no longer managed");
                }
            }
        });
    }

    // Called from class ClientApp
    public void connectToServer(String loginRequest) {
        this.loginRequest = loginRequest;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            //String uri = Hub.getInstance().settings.publicServerURL_localTest;
            String uri = Hub.getInstance().settings.publicServerURL;
            container.connectToServer(WebSocketClient.class, URI.create(uri)); // returns a WebSocket session object
        } catch (Exception e) {
            System.out.println("Unable to connect to server"); // TODO: Don't use e.printStackTrace (Will eat memory)
        }
    }

    // Called from class Hub
    public void closeConnection() {
        synchronized (lockObject_close) {
            if (connectedToServer) {
                connectedToServer = false;
                loggedInToServer = false;
                manageConnThread.interrupt();
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
        if(!manageConnThread.isAlive()) {
            manageConnThread.start();
        }
        // Send login request
        writeToServer(loginRequest);
    }

    // Called from class WebSocketClient
    public void onServerClose() {
        connectedToServer = false;
        loggedInToServer = false;
        System.out.println("Server closed the session. Will attempt reconnection in 45 sec");
    }

    // Called from class ClientApp
    public void writeToServer(String msg) {
        synchronized (lockObject_output) {
            try {
                if (connectedToServer) {
                    if(session.isOpen()) {  //TODO: Need to have this separated from the above if?
                        debugLog("Msg to server: " + msg);
                        session.getBasicRemote().sendText(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Called from class WebSocketClient
    public void newCommandFromServer(String msg) {
        debugLog("Msg from server: " + msg);
        try {
            Hub.getInstance().requests.put(msg);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    // Automatic ping and reconnect
    // Run by worker thread. Closed by interrupting it (interrupt sleep -> throws exception).
    private void manageConnection() throws Exception {
        long pingInterval = 45 * 1000;
        while(!Hub.getInstance().terminate) {
            Thread.sleep(pingInterval);
            if(loggedInToServer) {
                writeToServer("ping");
            } else {
                if(!connectedToServer) {
                    connectToServer(loginRequest);
                }
            }
        }
    }

    private synchronized void debugLog(String log) {
        Hub.getInstance().debugLog(log);
    }
}
