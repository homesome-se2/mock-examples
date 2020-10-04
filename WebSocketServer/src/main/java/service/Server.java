package service;

import model.ClientRequest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {

    public BlockingQueue<ClientRequest> clientRequests;

    // Make Singleton
    private static Server instance = null;

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    private Server(){
        clientRequests = new ArrayBlockingQueue<ClientRequest>(10);
    }

    public void launchServer() {
        ClientHandler.getInstance().launchWebSocketServer();
        processRequests();
    }

    public void stopServer() {
        ClientHandler.getInstance().stopWebSocketServer();
    }

    private void processRequests() {
        while (true) {
            try {
                ClientRequest clientRequest = clientRequests.take();
                int issuingClient = clientRequest.sessionID;
                String msg = clientRequest.msg;
                System.out.println("Request from " + issuingClient + ": " + msg);
                // ...
            } catch (Exception e) {
                // Ignore
            }
        }

    }

}
