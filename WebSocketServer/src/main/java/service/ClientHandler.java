package service;

import model.Client;
import model.ClientRequest;
import org.eclipse.jetty.websocket.api.Session;
import resource.WebSocketServer;
import spark.Spark;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler {

    private Map<Session, Client> connectedClients;

    private final Object lockObject_addRemove;
    private final Object lockObject_clientReq;
    private final Object lockObject_output;

    // Make Singleton
    private static ClientHandler instance = null;

    public static ClientHandler getInstance() {
        if (instance == null) {
            instance = new ClientHandler();
        }
        return instance;
    }

    private ClientHandler(){
        connectedClients = new ConcurrentHashMap<Session, Client>();
        lockObject_addRemove = new Object();
        lockObject_clientReq = new Object();
        lockObject_output = new Object();
    }

    public void launchWebSocketServer() {
        // Create web socket listening on a path, and being implemented by a class.
        Spark.webSocket("/test", WebSocketServer.class);
        Spark.port(8086);
        Spark.init();
        // Browser test: http://localhost:8086/
        // If no web page is provided, should say "404 Error, service powered by Jetty"

        System.out.println("Jetty WebSocket (web) server started");
    }

    public void stopWebSocketServer() {
        Spark.stop();
    }

    public void addClient(Session session) {
        synchronized (lockObject_addRemove) {
            System.out.println("New client connection");
            if (connectedClients.size() <= 10) {
                Client newClient = new Client();
                connectedClients.put(session, newClient);
            } else {
                System.out.println("Client limit reached.");
            }
            System.out.println("Number of clients: " + connectedClients.size());
        }
    }

    public void removeClient(Session session) {
        synchronized (lockObject_addRemove) {
            connectedClients.remove(session);
            System.out.println("Client disconnected");
            System.out.println("Number of clients: " + connectedClients.size());
        }
    }

    public void addClientRequest(Session session, String request) {
        synchronized (lockObject_clientReq) {
            System.out.println("Request from client id " + request);
            try {
                if (connectedClients.get(session).loggedIn) {
                    // Add request to server
                    ClientRequest newRequest = new ClientRequest(connectedClients.get(session).sessionID, request);
                    Server.getInstance().clientRequests.put(newRequest);
                } else {
                    clientLogin(session, request);
                }
            } catch (Exception e) {
                System.out.println("Unable to handle request: " + request);
            }
        }
    }

    public void clientLogin(Session session, String loginRequest) {
        System.out.println("Client logging in");
        // Pretend client logs in by sending his/her name:
        Client client = connectedClients.get(session);
        client.name = loginRequest;
        client.loggedIn = true;
        // Notify all clients
        outputToClients(session, false, "New client logged in: " + client.name +
                ". sessionID: " + client.sessionID);

    }

    public void outputToClients(Session issuingClient, boolean onlyToIndividual, String msg) {
        synchronized (lockObject_output) {
            System.out.println("Output to " + (onlyToIndividual ? "client: " : "clients: ") + msg);
            if (onlyToIndividual) {
                if (issuingClient.isOpen()) {
                    try {
                        issuingClient.getRemote().sendString(msg);
                    } catch (IOException e) {
                        System.out.println("Unable to write to client");
                    }
                } else {
                    System.out.println("Client session closed");
                }
            } else {
                // Send to all connected & logged in
                for (Session session : connectedClients.keySet()) {
                    if (connectedClients.get(session).loggedIn) {
                        try {
                            if (session.isOpen()) {
                                session.getRemote().sendString(msg);
                            } else
                                System.out.println("Client session closed");
                        } catch (IOException e) {
                            System.out.println("Unable to write to client");
                            // Maybe remove client from list??
                        }
                    }
                }
            }
        }
    }
}
